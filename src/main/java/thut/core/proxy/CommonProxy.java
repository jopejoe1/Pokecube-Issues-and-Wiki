package thut.core.proxy;

import java.util.Locale;
import java.util.stream.Stream;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.api.LinkableCaps;
import thut.api.TickHandler;
import thut.api.Tracker;
import thut.api.maths.Vector3;
import thut.api.terrain.BiomeType;
import thut.api.terrain.StructureManager;
import thut.api.terrain.TerrainManager;
import thut.core.common.Proxy;
import thut.core.common.ThutCore.MobEvents;
import thut.core.common.world.mobs.data.SyncHandler;

public class CommonProxy implements Proxy
{
    public static final String SET_SUBBIOME = "thutcore.subbiome.set";

    @Override
    public void setup(final FMLCommonSetupEvent event)
    {
        Proxy.super.setup(event);

        // Setup terrain manager
        TerrainManager.getInstance();

        MinecraftForge.EVENT_BUS.register(LinkableCaps.class);
        MinecraftForge.EVENT_BUS.register(TerrainManager.class);
        MinecraftForge.EVENT_BUS.register(StructureManager.class);
        MinecraftForge.EVENT_BUS.register(TickHandler.class);
        MinecraftForge.EVENT_BUS.register(MobEvents.class);
        MinecraftForge.EVENT_BUS.register(SyncHandler.class);

        MinecraftForge.EVENT_BUS.register(this);

        TerrainManager.init();

        PermissionAPI.registerNode(CommonProxy.SET_SUBBIOME, DefaultPermissionLevel.OP,
                "Able to set subbiomes via items");
    }

    BiomeType getSubbiome(final ServerPlayer player, final ItemStack held)
    {
        if (!PermissionAPI.hasPermission(player, CommonProxy.SET_SUBBIOME)) return null;
        if (held.getHoverName().getString().toLowerCase(Locale.ROOT).startsWith("subbiome->"))
        {
            final String[] args = held.getHoverName().getString().split("->");
            if (args.length != 2) return null;
            return BiomeType.getBiome(args[1].trim());
        }
        return null;
    }

    protected boolean isSubbiomeEditor(final ServerPlayer player, final ItemStack held)
    {
        return this.getSubbiome(player, held) != null;
    }

    @SubscribeEvent
    public void interactRightClickBlock(final PlayerInteractEvent.RightClickBlock evt)
    {
        if (evt.getHand() == InteractionHand.OFF_HAND || !(evt.getPlayer() instanceof ServerPlayer) || evt
                .getItemStack().isEmpty() || !evt.getPlayer().isShiftKeyDown() || !this.isSubbiomeEditor(
                        (ServerPlayer) evt.getPlayer(), evt.getItemStack())) return;
        final ItemStack itemstack = evt.getItemStack();
        final Player playerIn = evt.getPlayer();
        final Level worldIn = evt.getWorld();
        final BlockPos pos = evt.getPos();
        if (itemstack.hasTag() && playerIn.isShiftKeyDown() && itemstack.getTag().contains("min"))
        {
            final CompoundTag minTag = itemstack.getTag().getCompound("min");
            final BlockPos min = pos;
            final BlockPos max = Vector3.readFromNBT(minTag, "").getPos();
            if (!worldIn.isClientSide)
            {
                final BiomeType subbiome = this.getSubbiome((ServerPlayer) evt.getPlayer(), itemstack);
                final BoundingBox box = BoundingBox.fromCorners(min, max);
                final Stream<BlockPos> poses = BlockPos.betweenClosedStream(box.minX, box.minY, box.minZ, box.maxX,
                        box.maxY, box.maxZ);
                poses.forEach((p) ->
                {
                    TerrainManager.getInstance().getTerrain(worldIn, p).setBiome(p, subbiome);
                });
                final String message = "msg.subbiome.set";
                playerIn.sendMessage(new TranslatableComponent(message, subbiome.name), Util.NIL_UUID);
            }
            itemstack.getTag().remove("min");
            evt.setCanceled(true);
        }
        else
        {
            if (!itemstack.hasTag()) itemstack.setTag(new CompoundTag());
            final CompoundTag min = new CompoundTag();
            Vector3.getNewVector().set(pos).writeToNBT(min, "");
            itemstack.getTag().put("min", min);
            final String message = "msg.subbiome.setcorner";
            if (!worldIn.isClientSide) playerIn.sendMessage(new TranslatableComponent(message, pos), Util.NIL_UUID);
            evt.setCanceled(true);
            itemstack.getTag().putLong("time", Tracker.instance().getTick());
        }
    }

    @SubscribeEvent
    public void interactRightClickBlock(final PlayerInteractEvent.RightClickItem evt)
    {
        if (evt.getHand() == InteractionHand.OFF_HAND || !(evt.getPlayer() instanceof ServerPlayer) || evt
                .getItemStack().isEmpty() || !evt.getPlayer().isShiftKeyDown() || !this.isSubbiomeEditor(
                        (ServerPlayer) evt.getPlayer(), evt.getItemStack())) return;
        final ItemStack itemstack = evt.getItemStack();
        final Player playerIn = evt.getPlayer();
        final Level worldIn = evt.getWorld();
        final long now = Tracker.instance().getTick();
        if (itemstack.hasTag() && playerIn.isShiftKeyDown() && itemstack.getTag().contains("min") && itemstack.getTag()
                .getLong("time") != now)
        {
            final CompoundTag minTag = itemstack.getTag().getCompound("min");
            final Vec3 loc = playerIn.position().add(0, playerIn.getEyeHeight(), 0).add(playerIn.getLookAngle().scale(
                    2));
            final BlockPos pos = new BlockPos(loc);
            final BlockPos min = pos;
            final BlockPos max = Vector3.readFromNBT(minTag, "").getPos();
            if (!worldIn.isClientSide)
            {
                final BiomeType subbiome = this.getSubbiome((ServerPlayer) evt.getPlayer(), itemstack);
                final BoundingBox box = BoundingBox.fromCorners(min, max);
                final Stream<BlockPos> poses = BlockPos.betweenClosedStream(box.minX, box.minY, box.minZ, box.maxX,
                        box.maxY, box.maxZ);
                poses.forEach((p) ->
                {
                    TerrainManager.getInstance().getTerrain(worldIn, p).setBiome(p, subbiome);
                });
                final String message = "msg.subbiome.set";
                playerIn.sendMessage(new TranslatableComponent(message, subbiome.name), Util.NIL_UUID);
            }
            itemstack.getTag().remove("min");
        }
    }
}
