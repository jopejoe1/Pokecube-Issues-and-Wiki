package pokecube.core.ai.npc.behaviours;

import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InteractWith<E extends LivingEntity, T extends LivingEntity> extends Behavior<E>
{
    private final int maxDist;
    private final float speedModifier;
    private final Predicate<LivingEntity> type;
    private final int interactionRangeSqr;
    private final Predicate<LivingEntity> targetFilter;
    private final Predicate<LivingEntity> selfFilter;
    private final MemoryModuleType<LivingEntity> memory;

    public InteractWith(Predicate<LivingEntity> validUser, int p_23247_, Predicate<LivingEntity> selfFiler,
            Predicate<LivingEntity> targetFilter, MemoryModuleType<LivingEntity> memory, float p_23251_, int p_23252_)
    {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.WALK_TARGET,
                MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                MemoryStatus.VALUE_PRESENT));
        this.type = validUser;
        this.speedModifier = p_23251_;
        this.interactionRangeSqr = p_23247_ * p_23247_;
        this.maxDist = p_23252_;
        this.targetFilter = targetFilter;
        this.selfFilter = selfFiler;
        this.memory = memory;
    }

    public static <T extends LivingEntity> InteractWith<LivingEntity, T> of(Predicate<LivingEntity> p_23261_,
            int p_23262_, MemoryModuleType<LivingEntity> p_23263_, float p_23264_, int p_23265_)
    {
        return new InteractWith<>(p_23261_, p_23262_, (p_23287_) -> {
            return true;
        }, (p_23285_) -> {
            return true;
        }, p_23263_, p_23264_, p_23265_);
    }

    public static <T extends LivingEntity> InteractWith<LivingEntity, T> of(Predicate<LivingEntity> p_147567_,
            int p_147568_, Predicate<LivingEntity> p_147569_, MemoryModuleType<LivingEntity> p_147570_, float p_147571_,
            int p_147572_)
    {
        return new InteractWith<>(p_147567_, p_147568_, (p_147584_) -> {
            return true;
        }, p_147569_, p_147570_, p_147571_, p_147572_);
    }

    protected boolean checkExtraStartConditions(ServerLevel p_23254_, E p_23255_)
    {
        return this.selfFilter.test(p_23255_) && this.seesAtLeastOneValidTarget(p_23255_);
    }

    private boolean seesAtLeastOneValidTarget(E p_23267_)
    {
        NearestVisibleLivingEntities nearestvisiblelivingentities = p_23267_.getBrain()
                .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();
        return nearestvisiblelivingentities.contains(this::isTargetValid);
    }

    private boolean isTargetValid(LivingEntity p_23279_)
    {
        return this.type.test(p_23279_) && this.targetFilter.test(p_23279_);
    }

    protected void start(ServerLevel p_23257_, E p_23258_, long p_23259_)
    {
        Brain<?> brain = p_23258_.getBrain();
        Optional<NearestVisibleLivingEntities> optional = brain
                .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (!optional.isEmpty())
        {
            NearestVisibleLivingEntities nearestvisiblelivingentities = optional.get();
            nearestvisiblelivingentities.findClosest((p_186046_) -> {
                return this.canInteract(p_23258_, p_186046_);
            }).ifPresent((p_186043_) -> {
                brain.setMemory(this.memory, p_186043_);
                brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(p_186043_, true));
                brain.setMemory(MemoryModuleType.WALK_TARGET,
                        new WalkTarget(new EntityTracker(p_186043_, false), this.speedModifier, this.maxDist));
            });
        }
    }

    private boolean canInteract(E p_186039_, LivingEntity p_186040_)
    {
        return this.type.test(p_186040_) && p_186040_.distanceToSqr(p_186039_) <= (double) this.interactionRangeSqr
                && this.targetFilter.test(p_186040_);
    }
}