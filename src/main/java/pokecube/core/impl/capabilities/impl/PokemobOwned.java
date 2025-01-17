package pokecube.core.impl.capabilities.impl;

import java.util.UUID;

import com.google.common.collect.Lists;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pokecube.api.PokecubeAPI;
import pokecube.api.data.abilities.Ability;
import pokecube.api.data.abilities.AbilityManager;
import pokecube.api.entity.TeamManager;
import pokecube.api.entity.pokemob.IPokemob;
import pokecube.api.entity.pokemob.PokemobCaps;
import pokecube.api.entity.pokemob.ai.CombatStates;
import pokecube.api.entity.pokemob.ai.GeneralStates;
import pokecube.api.events.PCEvent;
import pokecube.api.events.pokemobs.RecallEvent;
import pokecube.api.events.pokemobs.SpawnEvent;
import pokecube.api.events.pokemobs.SpawnEvent.SpawnContext;
import pokecube.api.events.pokemobs.combat.MoveMessageEvent;
import pokecube.api.utils.TagNames;
import pokecube.core.PokecubeCore;
import pokecube.core.ai.brain.BrainUtils;
import pokecube.core.ai.logic.LogicMountedControl;
import pokecube.core.database.pokedex.PokedexEntryLoader.SpawnRule;
import pokecube.core.entity.pokemobs.AnimalChest;
import pokecube.core.eventhandlers.EventsHandler;
import pokecube.core.eventhandlers.SpawnHandler;
import pokecube.core.eventhandlers.StatsCollector;
import pokecube.core.handlers.playerdata.PlayerPokemobCache;
import pokecube.core.impl.PokecubeMod;
import pokecube.core.init.EntityTypes;
import pokecube.core.items.pokecubes.EntityPokecube;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.network.pokemobs.PacketPokemobMessage;
import pokecube.core.network.pokemobs.PokemobPacketHandler.MessageServer;
import pokecube.core.utils.CapHolders;
import pokecube.core.utils.Tools;
import thut.lib.TComponent;

public abstract class PokemobOwned extends PokemobAI implements ContainerListener
{

    @Override
    public void displayMessageToOwner(final Component message)
    {
        final Entity owner = this.getOwner();
        // Ensure this is actually client side before sending this.
        if (owner instanceof ServerPlayer player && this.getEntity().isAlive())
        {
            if (PokecubeMod.debug) PokecubeAPI.LOGGER.info(message.getString());
            final MoveMessageEvent event = new MoveMessageEvent(this, message);
            PokecubeAPI.MOVE_BUS.post(event);
            PacketPokemobMessage.sendMessage(player, event.message);
        }
    }

    @Override
    public LogicMountedControl getController()
    {
        return this.controller;
    }

    @Override
    public int getDyeColour()
    {
        int info = this.dataSync().get(this.params.DYECOLOUR);
        if (info == -1)
            info = this.isShiny() ? this.getPokedexEntry().defaultSpecials : this.getPokedexEntry().defaultSpecial;
        return info;
    }

    @Override
    public BlockPos getHome()
    {
        if (this.guardCap.getActiveTask() != null) return this.guardCap.getActiveTask().getPos();
        return this.guardCap.getPrimaryTask().getPos();
    }

    @Override
    public float getHomeDistance()
    {
        if (this.guardCap.getActiveTask() != null) return this.guardCap.getActiveTask().getRoamDistance();
        return this.guardCap.getPrimaryTask().getRoamDistance();
    }

    @Override
    public AnimalChest getInventory()
    {
        if (this.pokeChest == null) this.initInventory();
        return this.pokeChest;
    }

    @Override
    public UUID getOriginalOwnerUUID()
    {
        return this.OTID;
    }

    @Override
    public LivingEntity getOwner()
    {
        final UUID ownerID = this.getOwnerId();
        if (ownerID == null) return null;
        final Level world = this.getEntity().getLevel();
        final boolean serv = world instanceof ServerLevel;
        if (!serv && ownerID.equals(PokecubeCore.proxy.getPlayer().getUUID()))
            if (this.getOwnerHolder().getOwner() == null)
                this.getOwnerHolder().setOwner(PokecubeCore.proxy.getPlayer());
        return serv ? this.getOwnerHolder().getOwner((ServerLevel) world) : this.getOwnerHolder().getOwner();
    }

    @Override
    public UUID getOwnerId()
    {
        return this.getOwnerHolder().getOwnerId();
    }

    @Override
    public String getPokemobTeam()
    {
        if (this.team.isEmpty()) this.team = TeamManager.getTeam(this.getEntity());
        return this.team;
    }

    @Override
    public boolean hasHomeArea()
    {
        return this.getHome() != null && this.getHomeDistance() > 0;
    }

    protected void initInventory()
    {
        AnimalChest animalchest = this.pokeChest;
        this.pokeChest = new AnimalChest();
        if (animalchest != null)
        {
            animalchest.removeListener(this);
            final int i = Math.min(animalchest.getContainerSize(), this.pokeChest.getContainerSize());
            for (int j = 0; j < i; ++j)
            {
                final ItemStack itemstack = animalchest.getItem(j);
                if (itemstack != ItemStack.EMPTY) this.pokeChest.setItem(j, itemstack.copy());
            }
            animalchest = null;
        }
        this.pokeChest.addListener(this);
    }

    @Override
    public boolean isPlayerOwned()
    {
        return this.getOwnerHolder().isPlayerOwned();
    }

    @Override
    public boolean moveToShoulder(final Player player)
    {
        final float scale = this.getSize();
        final float width = this.getPokedexEntry().width * scale;
        final float height = this.getPokedexEntry().height * scale;
        final float length = this.getPokedexEntry().length * scale;
        boolean rightSize = width < 1 && height < 1 && length < 1;
        rightSize |= this.getPokedexEntry().canSitShoulder;
        if (!rightSize) return false;
        if (super.moveToShoulder(player))
        {
            this.returning = true;
            return true;
        }
        return false;
    }

    @Override
    public void containerChanged(final Container inventory)
    {}

    @Override
    public void onRecall(final boolean onDeath)
    {
        if (this.isRemoved())
        {
            this.getEntity().discard();
            return;
        }
        // We use this directly as isAlive() also checks hp!
        final boolean removed = this.getEntity().isRemoved();
        if (removed) return;
        if (this.getOwnerId() == null)
        {
            this.getEntity().discard();
            return;
        }
        if (!(this.getEntity().getLevel() instanceof ServerLevel))
        {
            final MessageServer packet = new MessageServer(MessageServer.RETURN, this.getEntity().getId());
            PokecubeCore.packets.sendToServer(packet);
            return;
        }
        else this.executeRecall();
    }

    protected void executeRecall()
    {
        final UUID id = this.getEntity().getUUID();
        final Entity mob = this.getEntity();
        final Level world = this.getEntity().getLevel();
        final BlockPos pos = this.getEntity().blockPosition();
        // Ensures the chunk is actually still loaded here.
        world.getChunk(pos);
        if (this.getTransformedTo() != null) this.setTransformedTo(null);
        if (this.getBossInfo() != null)
        {
            this.getBossInfo().removeAllPlayers();
            this.getBossInfo().setVisible(false);
        }
        final RecallEvent pre = new RecallEvent.Pre(this);
        PokecubeAPI.POKEMOB_BUS.post(pre);
        if (pre.isCanceled()) return;
        final RecallEvent evtrec = new RecallEvent.Post(this);
        PokecubeAPI.POKEMOB_BUS.post(evtrec);
        if (this.getHealth() > 0 && evtrec.isCanceled()) return;
        this.setEvolutionTicks(0);
        this.setGeneralState(GeneralStates.EXITINGCUBE, false);
        this.setGeneralState(GeneralStates.EVOLVING, false);
        this.setCombatState(CombatStates.DYNAMAX, false);

        if (this.returning)
        {
            this.getEntity().discard();
            return;
        }

        this.returning = true;

        final boolean megaForm = this.getCombatState(CombatStates.MEGAFORME) || this.getPokedexEntry().isMega();

        IPokemob base = this;
        if (megaForm) base = this.megaRevert();

        final Ability ab = this.getAbility();
        if (ab != null) base = ab.onRecall(base);

        if (base != this && base != null)
        {
            final float hp = this.getHealth();
            base.setHealth(hp);
            if (base == this) this.returning = false;
            if (this.getEntity().getPersistentData().contains(TagNames.ABILITY)) base.setAbilityRaw(
                    AbilityManager.getAbility(this.getEntity().getPersistentData().getString(TagNames.ABILITY)));
            base.onRecall();
            this.getEntity().getPersistentData().putBoolean(TagNames.REMOVED, true);
            this.getEntity().getPersistentData().putBoolean(TagNames.CAPTURING, true);
            this.getEntity().captureDrops(null);
            this.getEntity().discard();
            EventsHandler.Schedule(world, w -> {
                final ServerLevel srld = (ServerLevel) w;
                final Entity original = srld.getEntity(id);
                if (original == mob) original.setRemoved(RemovalReason.DISCARDED);
                return true;
            });
            return;
        }

        if (PokecubeMod.debug) PokecubeAPI.LOGGER.info("Recalling " + this.getEntity());
        // Clear the pokemob's motion on recall
        this.getEntity().setDeltaMovement(0, 0, 0);

        /** If this has fainted, status should be reset. */
        if (this.getHealth() <= 0)
        {
            this.healStatus();
            this.healChanges();
        }

        final Entity owner = this.getOwner();

        this.setCombatState(CombatStates.NOMOVESWAP, false);
        this.setCombatState(CombatStates.ANGRY, false);

        // Reset this so that the ability shows correctly on the cube.
        this.timeSinceCombat = -50;

        this.getEntity().captureDrops(Lists.newArrayList());
        final Player tosser = PokecubeMod.getFakePlayer(this.getEntity().getLevel());

        boolean added = false;
        toPlayer:
        if (owner instanceof Player player)
        {
            final ItemStack itemstack = PokecubeManager.pokemobToItem(this);
            boolean noRoom = false;
            final boolean ownerDead = player.getHealth() <= 0;
            if (ownerDead || player.getInventory().getFreeSlot() == -1) noRoom = true;
            if (noRoom) break toPlayer;
            else
            {
                added = player.getInventory().add(itemstack);
                if (!added) break toPlayer;
            }
            if (!owner.isShiftKeyDown() && this.getEntity().isAlive() && !ownerDead)
            {
                boolean has = StatsCollector.getCaptured(this.getPokedexEntry(), player) > 0;
                has = has || StatsCollector.getHatched(this.getPokedexEntry(), player) > 0;
                if (!has) StatsCollector.addCapture(this);
            }
            final Component mess = TComponent.translatable("pokemob.action.return", this.getDisplayName());
            this.displayMessageToOwner(mess);
        }
        if (!added && this.getOwnerId() != null)
        {
            final ItemStack itemstack = PokecubeManager.pokemobToItem(this);
            final PCEvent event = new PCEvent(world, itemstack.copy(), this.getOwnerId(), this.isPlayerOwned());
            PokecubeAPI.POKEMOB_BUS.post(event);
            if (!event.isCanceled()) this.onToss(tosser, itemstack.copy());
        }

        // This ensures it can't be caught by dupe
        this.getEntity().getPersistentData().putBoolean(TagNames.REMOVED, true);
        this.getEntity().getPersistentData().putBoolean(TagNames.CAPTURING, true);
        this.getEntity().captureDrops(null);
        this.getEntity().discard();

        final LivingEntity targ = BrainUtils.getAttackTarget(this.getEntity());
        /**
         * If we have a target, and we were recalled with health, assign the
         * target to our owner instead.
         */
        if (this.getCombatState(CombatStates.ANGRY) && targ != null && this.getHealth() > 0)
            if (owner instanceof LivingEntity)
        {
            final IPokemob targetMob = PokemobCaps.getPokemobFor(targ);
            if (targetMob != null)
            {
                BrainUtils.initiateCombat(targetMob.getEntity(), this.getOwner());
                if (PokecubeMod.debug) PokecubeAPI.LOGGER.info("Swapping agro to cowardly owner!");
            }
            else targ.setLastHurtByMob(this.getOwner());
        }

        EventsHandler.Schedule(world, w -> {
            final ServerLevel srld = (ServerLevel) w;
            final Entity original = srld.getEntity(id);
            if (original == mob) original.setRemoved(RemovalReason.DISCARDED);
            return true;
        });
    }

    private void onToss(final LivingEntity owner, final ItemStack itemstack)
    {
        final EntityPokecube entity = new EntityPokecube(EntityTypes.getPokecube(), owner.getLevel());
        entity.shootingEntity = owner;
        entity.shooter = owner.getUUID();
        entity.setItem(itemstack);
        this.here.set(this.getEntity());
        this.here.moveEntity(entity);
        this.here.clear().setVelocities(entity);
        entity.targetEntity = null;
        entity.targetLocation.clear();
        this.getEntity().getLevel().addFreshEntity(entity);
    }

    @Override
    public void setDyeColour(final int info)
    {
        this.dataSync().set(this.params.DYECOLOUR, Integer.valueOf(info));
    }

    @Override
    public void setHeldItem(final ItemStack itemStack)
    {
        final ItemStack oldStack = this.getHeldItem();
        this.getInventory().setItem(1, itemStack);
        this.getPokedexEntry().onHeldItemChange(oldStack, itemStack, this);
        super.setHeldItem(itemStack);
        this.dataSync().set(this.params.HELDITEMDW, itemStack);
        // Now check if we need to cancel any mega evolutions, etc.
        // megaRevert handles checking if we are mega evolved, etc
        if (!itemStack.isEmpty()) this.megaRevert();
    }

    @Override
    public void setHome(final int x, final int y, final int z, final int distance)
    {
        if (this.guardCap == null) // First try to collect and reset this
            this.guardCap = this.entity.getCapability(CapHolders.GUARDAI_CAP).orElse(null);

        // Then lets just log the error
        if (this.guardCap == null || this.guardCap.getPrimaryTask() == null)
        {
            PokecubeAPI.LOGGER.error("Error with setting home! {}", this.guardCap);
            return;
        }
        this.guardCap.getPrimaryTask().setPos(new BlockPos(x, y, z));
        this.guardCap.getPrimaryTask().setRoamDistance(distance);
    }

    @Override
    public void setOriginalOwnerUUID(final UUID original)
    {
        this.OTID = original;
    }

    @Override
    public void setOwner(final LivingEntity e)
    {
        if (e == null)
        {
            // Clear team
            this.setPokemobTeam("");
            // Clear uuid
            this.getOwnerHolder().setOwner((UUID) null);
            /*
             * unset tame.
             */
            this.setGeneralState(GeneralStates.TAMED, false);
            return;
        }
        /*
         * Set it as tame.
         */
        this.setGeneralState(GeneralStates.TAMED, true);
        /*
         * Set not to wander around by default, they can choose to enable this
         * later.
         */
        this.setRoutineState(AIRoutine.WANDER, false);
        /*
         * Set owner, and set original owner if none already exists.
         */
        this.getOwnerHolder().setOwner(e);
        this.getOwnerHolder().setOwner(e.getUUID());
        if (this.getOriginalOwnerUUID() == null) this.setOriginalOwnerUUID(e.getUUID());
        /*
         * Trigger vanilla event for taming a mob.
         */
        if (e instanceof ServerPlayer player && this.getEntity() instanceof Animal animal)
            CriteriaTriggers.TAME_ANIMAL.trigger(player, animal);
    }

    @Override
    public void setOwner(final UUID owner)
    {
        final UUID old = this.getOwnerId();
        if (old != null && owner != null) PlayerPokemobCache.RemoveFromCache(old, this);
        this.getOwnerHolder().setOwner(owner);
        // Clear team, it will refresh it whenever it is actually checked.
        this.setPokemobTeam("");
        if (this.getEntity() instanceof TamableAnimal animal) animal.setOwnerUUID(owner);
        if (owner != null) PlayerPokemobCache.UpdateCache(this);
    }

    @Override
    public void setPokemobTeam(final String team)
    {
        this.team = team;
    }

    @Override
    public IPokemob spawnInit(final SpawnRule info)
    {
        this.resetLoveStatus();
        final IPokemob pokemob = this;
        this.spawnInitRule = info;
        FormeHolder forme = this.getCustomHolder();
        if (forme != null)
        {
            // Sync these to PGs as well
            this.genesSpecies.getAllele(0).getValue().forme = forme;
            this.genesSpecies.getAllele(1).getValue().forme = forme;
        }
        return pokemob;
    }

    @Override
    public IPokemob onAddedInit()
    {
        IPokemob pokemob = this;
        if (this.spawnInitRule == null) return this;
        int maxXP = pokemob.getEntity().getPersistentData().getInt("spawnExp");
        /*
         * Check to see if the mob has spawnExp defined in its data. If not, it
         * will choose how much exp it spawns with based on the position that it
         * spawns in worls with.
         */
        if (maxXP == 0)
        {
            if (!pokemob.getEntity().getPersistentData().getBoolean("initSpawn"))
            {
                // Only set this if we haven't had one set yet already
                if (pokemob.getHeldItem().isEmpty()) pokemob.setHeldItem(pokemob.wildHeldItem(pokemob.getEntity()));
                if (pokemob instanceof PokemobOwned mob) mob.updateHealth();
                pokemob.setHealth(pokemob.getMaxHealth());
                return pokemob;
            }
            pokemob.getEntity().getPersistentData().remove("initSpawn");
            maxXP = SpawnHandler.getSpawnXp(new SpawnContext(pokemob));
            final SpawnEvent.PickLevel event = new SpawnEvent.PickLevel(pokemob,
                    Tools.xpToLevel(pokemob.getPokedexEntry().getEvolutionMode(), -1), SpawnHandler.DEFAULT_VARIANCE);
            PokecubeAPI.POKEMOB_BUS.post(event);
            final int level = event.getLevel();
            maxXP = Tools.levelToXp(pokemob.getPokedexEntry().getEvolutionMode(), level);
        }
        this.getEntity().getPersistentData().remove("spawnExp");

        // Set exp and held items.
        pokemob = pokemob.setForSpawn(maxXP);
        // Only set this if we haven't had one set yet already
        if (pokemob.getHeldItem().isEmpty()) pokemob.setHeldItem(pokemob.wildHeldItem(pokemob.getEntity()));

        // Make sure heath is valid numbers.
        if (pokemob instanceof PokemobOwned mob) mob.updateHealth();
        pokemob.getEntity().setHealth(pokemob.getEntity().getMaxHealth());

        // If we have some spawn info, lets process it.
        if (this.spawnInitRule != null)
        {
            final FormeHolder holder = this.spawnInitRule.getForme(pokemob.getPokedexEntry());
            if (holder != null) pokemob.setCustomHolder(holder);
        }
        if (pokemob != this) pokemob.spawnInit(this.spawnInitRule);
        return pokemob;
    };

    @Override
    public void markRemoved()
    {
        this.isRemoved = true;
    }

    @Override
    public boolean isRemoved()
    {
        return this.isRemoved;
    }
}
