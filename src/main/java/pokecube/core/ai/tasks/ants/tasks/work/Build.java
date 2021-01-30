package pokecube.core.ai.tasks.ants.tasks.work;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.collect.Maps;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.items.IItemHandlerModifiable;
import pokecube.core.PokecubeCore;
import pokecube.core.ai.tasks.ants.AntTasks.AntJob;
import pokecube.core.ai.tasks.ants.AntTasks.AntRoom;
import pokecube.core.ai.tasks.ants.nest.Edge;
import pokecube.core.ai.tasks.ants.nest.Node;
import pokecube.core.ai.tasks.ants.nest.Part;
import pokecube.core.ai.tasks.ants.nest.Tree;
import pokecube.core.ai.tasks.ants.tasks.AbstractConstructTask;
import pokecube.core.ai.tasks.ants.tasks.AbstractWorkTask;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.world.terrain.PokecubeTerrainChecker;

public class Build extends AbstractConstructTask
{
    public static interface IRoomHandler
    {
        default boolean validWall(final Tree tree, final ServerWorld world, final BlockPos pos,
                final AbstractWorkTask task, final boolean checkAir)
        {
            final Node node = tree.getEffectiveNode(pos, null);
            // No node, everything is a valid wall!
            if (node == null) return true;
            return this.validWall(node, world, pos, task, checkAir);
        }

        default boolean place(final Tree tree, final ServerWorld world, final BlockPos pos, final BlockState state,
                final AbstractWorkTask task)
        {
            final Node node = tree.getEffectiveNode(pos, null);
            // No node, don't place
            if (node == null) return false;
            return this.place(node, world, state, pos, task);
        }

        default boolean validWall(final Node node, final ServerWorld world, final BlockPos pos,
                final AbstractWorkTask task, final boolean checkAir)
        {
            final BlockState state = world.getBlockState(pos);
            final BlockPos mid = node.getCenter();
            final int dy = pos.getY() - mid.getY();
            final int dx = pos.getX() - mid.getX();
            final int dz = pos.getZ() - mid.getZ();

            boolean light = false;
            final SharedSeedRandom rng = new SharedSeedRandom();
            rng.setDecorationSeed(world.getSeed(), pos.getX(), pos.getZ());
            final boolean tryLight = rng.nextDouble() > 0.9 || dx == 0 && dz == 0;
            if (tryLight)
            {
                final BlockPos below = pos.down();
                final BlockPos above = pos.up();
                final boolean belowInside = node.getTree().isInside(below);
                final boolean aboveOnShell = node.getTree().isOnShell(above);
                if (belowInside && aboveOnShell) light = true;
            }

            final double dh2 = dx * dx + dz * dz;

            boolean valid = state.isOpaqueCube(world, pos) || state.getBlock() == Blocks.FARMLAND || state
                    .getBlock() == Blocks.SHROOMLIGHT;

            switch (node.type)
            {
            case ENTRANCE:
                // Entrances keep entrances on the cardinal directions
                boolean edge = dy >= 0 && dy < 2;
                edge = edge && (dx == 0 && dz > 1 && dz <= 4 || dz == 0 && dx > 1 && dx <= 4);
                if (edge && !light)
                {
                    edge = false;
                    for (final Edge e : node.edges)
                        if (e.isOnShell(pos))
                        {
                            edge = true;
                            break;
                        }
                    if (!edge) valid = world.isAirBlock(pos);
                }
                break;
            case FOOD:
                if (dx == 0 && dz == 0 && dy == -1) valid = world.getFluidState(pos).isTagged(FluidTags.WATER);
                else if (dx == 0 && dz == 0 && dy == -2) valid = state.getBlock() == Blocks.SHROOMLIGHT;
                else if (dy == -1 && dh2 <= node.size * node.size) valid = state.getBlock() == Blocks.FARMLAND;
                break;
            default:
                break;
            }
            if (light) valid = state.getBlock() == Blocks.SHROOMLIGHT;
            if (!valid && checkAir)
            {
                boolean wouldBeValid = false;
                if (task != null)
                {
                    task.tryHarvest(pos, true);
                    wouldBeValid = this.validWall(node, world, pos, null, false);
                }
                else if (AbstractWorkTask.diggable.test(state))
                {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 48);
                    wouldBeValid = this.validWall(node, world, pos, null, false);
                    world.setBlockState(pos, state, 48);
                }
                valid = wouldBeValid;
            }
            return valid;
        }

        default boolean place(final Node node, final ServerWorld world, BlockState state, final BlockPos pos,
                final AbstractWorkTask task)
        {
            final BlockPos mid = node.getCenter();
            final int dy = pos.getY() - mid.getY();
            final int dx = pos.getX() - mid.getX();
            final int dz = pos.getZ() - mid.getZ();

            boolean light = false;
            final SharedSeedRandom rng = new SharedSeedRandom();
            rng.setDecorationSeed(world.getSeed(), pos.getX(), pos.getZ());
            final boolean tryLight = rng.nextDouble() > 0.9 || dx == 0 && dz == 0;
            if (tryLight)
            {
                final BlockPos below = pos.down();
                final BlockPos above = pos.up();
                final boolean belowInside = node.getTree().isInside(below);
                final boolean aboveOnShell = node.getTree().isOnShell(above);
                if (belowInside && aboveOnShell) light = true;
            }
            if (light) state = Blocks.SHROOMLIGHT.getDefaultState();

            final double dh2 = dx * dx + dz * dz;
            switch (node.type)
            {
            case EGG:
                break;
            case ENTRANCE:
                boolean edge = dy >= 0 && dy < 2;
                edge = edge && (dx == 0 && dz > 1 && dz <= 4 || dz == 0 && dx > 1 && dx <= 4);
                if (edge && !light)
                {
                    edge = false;
                    for (final Edge e : node.edges)
                        if (e.isOnShell(pos))
                        {
                            edge = true;
                            break;
                        }
                    if (!edge) state = Blocks.AIR.getDefaultState();
                }
                break;
            case FOOD:
                if (dx == 0 && dz == 0 && dy == -1) state = Blocks.WATER.getDefaultState();
                else if (dx == 0 && dz == 0 && dy == -2) state = Blocks.SHROOMLIGHT.getDefaultState();
                else if (dy == -1 && dh2 <= node.size * node.size) state = Blocks.FARMLAND.getDefaultState();
                break;
            case NODE:
                break;
            default:
                break;
            }
            if (state.getBlock() == Blocks.AIR) return task == null ? world.destroyBlock(pos, true)
                    : task.tryHarvest(pos, true);
            else return world.setBlockState(pos, state);
        }
    }

    private static final IRoomHandler DEFAULT = new IRoomHandler()
    {
    };

    public static final Map<AntRoom, IRoomHandler> ROOMHANLDERS = Maps.newHashMap();

    ItemStack to_place = ItemStack.EMPTY;

    int storeInd = -1;

    boolean going_to_nest = false;

    public Build(final IPokemob pokemob)
    {
        super(pokemob, j -> j == AntJob.BUILD, 5);
    }

    private boolean buildPart(final Part part)
    {
        final long time = this.world.getGameTime();
        if (!part.shouldBuild(time)) return false;
        this.valids.set(0);
        final AntRoom type = AntRoom.NODE;
        final Tree tree = part.getTree();
        // Start with a check of if the pos is on the shell, this check is done
        // as it ensures that this is actually on the shell, and not say on a
        // border between rooms.
        Predicate<BlockPos> isValid = pos -> tree.isOnShell(pos);
        // If it is inside, and not diggable, we notify the node of the
        // dug spot, finally we check if there is space nearby to stand.
        isValid = isValid.and(pos ->
        {
            // This checks last time the block was attempted to be placed, if it
            // was too recent, we terminate here.
            if (!tree.shouldCheckBuild(pos, time)) return false;

            final Node node = part.getTree().getEffectiveNode(pos, part);
            final IRoomHandler handler = Build.ROOMHANLDERS.getOrDefault(node == part ? node.type : type,
                    Build.DEFAULT);
            final boolean wall = handler.validWall(tree, this.world, pos, null, true);
            if (!wall) this.valids.getAndIncrement();
            return !wall;
        });// .and(this.canStandNear);
           // For some reason, parallel stream fails here?
        final Optional<BlockPos> pos = part.getBuildBounds().stream().filter(isValid).findAny();
        if (pos.isPresent())
        {
            this.work_pos = pos.get();
            return true;
        }
        if (this.valids.get() == 0) part.build_done = time + 2400;
        return false;
    }

    private boolean divert(final Part old)
    {
        final long time = this.world.getGameTime();
        this.n = null;
        this.e = null;
        if (old instanceof Edge)
        {
            final Edge edge = (Edge) old;
            Node next = edge.node1;
            if (next.shouldBuild(time))
            {
                this.n = next;
                this.e = null;
                if (PokecubeMod.debug) PokecubeCore.LOGGER.debug("Switching to a node 1 b " + this.n.type);
                return true;
            }
            next = edge.node2;
            if (next.shouldBuild(time))
            {
                this.n = next;
                this.e = null;
                if (PokecubeMod.debug) PokecubeCore.LOGGER.debug("Switching to a node 2 b " + this.n.type);
                return true;
            }
        }
        else if (old instanceof Node)
        {
            final Node node = (Node) old;
            Edge next = null;
            for (final Edge e : node.edges)
                if (e.shouldBuild(time))
                {
                    next = e;
                    break;
                }
            if (next != null)
            {
                this.n = null;
                this.e = next;
                if (PokecubeMod.debug) PokecubeCore.LOGGER.debug("Switching to an edge 1 b");
                return true;
            }
        }
        // Try to find another open node or edge
        for (final Node n : this.nest.hab.rooms.allRooms)
            if (n.shouldBuild(time))
            {
                this.n = n;
                if (PokecubeMod.debug) PokecubeCore.LOGGER.debug("Switching to a node 3 b " + this.n.type);
                return true;
            }
        for (final Edge e : this.nest.hab.rooms.allEdges)
            if (e.shouldBuild(time))
            {
                this.e = e;
                if (PokecubeMod.debug) PokecubeCore.LOGGER.debug("Switching to an edge 2 b");
                return true;
            }
        return false;
    }

    private boolean hasItem()
    {
        // First. we need to check our own inventory, see if we have any blocks.
        // If so, we will place one of those. Otherwise, we need to go to the
        // nest, and pick one up from there.
        items:
        if (this.to_place.isEmpty())
        {
            for (int i = 2; i < this.pokemob.getInventory().getSizeInventory(); i++)
            {

                final ItemStack stack = this.pokemob.getInventory().getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem)
                {
                    final BlockItem item = (BlockItem) stack.getItem();
                    if (!PokecubeTerrainChecker.isTerrain(item.getBlock().getDefaultState())) continue;
                    this.storeInd = i;
                    this.to_place = stack;
                    this.progressTimer = -10;
                    break items;
                }
            }
            if (this.going_to_nest)
            {
                this.progressTimer = -60;
                final BlockPos pos = this.nest.nest.getPos();
                final IItemHandlerModifiable inv = this.storage.getInventory(this.world, pos, this.storage.storageFace);
                if (pos.distanceSq(this.entity.getPosition()) > 9) this.setWalkTo(pos, 1, 1);
                else
                {
                    for (int i = 0; i < inv.getSlots(); i++)
                    {
                        final ItemStack stack = inv.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem)
                        {
                            final BlockItem item = (BlockItem) stack.getItem();
                            if (!PokecubeTerrainChecker.isTerrain(item.getBlock().getDefaultState())) continue;
                            this.to_place = inv.extractItem(i, Math.min(stack.getCount(), 5), false);
                            this.storeInd = this.storage.firstEmpty;
                            this.pokemob.getInventory().setInventorySlotContents(this.storage.firstEmpty,
                                    this.to_place);
                            return false;
                        }
                    }
                    this.to_place = new ItemStack(Blocks.PODZOL, 5);
                    this.storeInd = this.storage.firstEmpty;
                    this.pokemob.getInventory().setInventorySlotContents(this.storage.firstEmpty, this.to_place);
                }
            }
            this.going_to_nest = this.to_place.isEmpty();
            if (this.going_to_nest)
            {
                this.progressTimer = -10;
                return false;
            }
        }
        return !this.to_place.isEmpty();
    }

    @Override
    protected boolean selectJobSite()
    {
        select:
        if (this.work_pos == null)
        {
            final boolean edge = this.e != null;
            final Part part = edge ? this.e : this.n;
            final long time = this.world.getGameTime();
            if (this.buildPart(part)) break select;
            if (!part.shouldBuild(time))
            {
                if (!this.divert(part)) this.endTask();
                return false;
            }
        }
        // Check item after selecting a postion, this way we can decide if the
        // item is the correct one to place at this location.
        return this.work_pos != null && this.hasItem();
    }

    @Override
    protected void doWork()
    {
        if (!this.to_place.isEmpty() && this.to_place.getItem() instanceof BlockItem && this.storeInd != -1)
        {
            final BlockItem item = (BlockItem) this.to_place.getItem();
            final BlockState state = item.getBlock().getDefaultState();
            boolean wall = false;
            final Part part = this.n == null ? this.e : this.n;
            final BlockPos pos = this.work_pos;
            final AntRoom type = AntRoom.NODE;
            final Tree tree = part.getTree();
            final Node node = tree.getEffectiveNode(pos, part);
            final IRoomHandler handler = Build.ROOMHANLDERS.getOrDefault(node == part ? node.type : type,
                    Build.DEFAULT);
            wall = handler.validWall(tree, this.world, pos, this, true);
            if (!wall)
            {
                handler.place(tree, this.world, this.work_pos, state, this);
                if (!this.world.isAirBlock(pos)) this.to_place.shrink(1);
            }
            part.markBuilt(this.work_pos, this.world.getGameTime() + 1200);
            this.pokemob.getInventory().setInventorySlotContents(this.storeInd, this.to_place);
        }
    }
}