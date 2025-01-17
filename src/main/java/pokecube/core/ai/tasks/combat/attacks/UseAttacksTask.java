package pokecube.core.ai.tasks.combat.attacks;

import org.apache.logging.log4j.Level;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraftforge.common.util.FakePlayer;
import pokecube.api.PokecubeAPI;
import pokecube.api.entity.pokemob.IPokemob;
import pokecube.api.entity.pokemob.PokemobCaps;
import pokecube.api.entity.pokemob.ai.CombatStates;
import pokecube.api.entity.pokemob.ai.GeneralStates;
import pokecube.api.moves.IMoveConstants;
import pokecube.api.moves.Move_Base;
import pokecube.core.PokecubeCore;
import pokecube.core.ai.brain.BrainUtils;
import pokecube.core.ai.tasks.combat.CombatTask;
import pokecube.core.items.pokecubes.EntityPokecubeBase;
import pokecube.core.moves.MovesUtils;
import thut.api.Tracker;
import thut.api.entity.ai.IAICombat;
import thut.api.maths.Vector3;
import thut.lib.TComponent;

/**
 * This is the IAIRunnable for managing which attack is used when. It determines
 * whether the pokemob is in range, manages pathing to account for range issues,
 * and also manage auto selection of moves for wild or hunting pokemobs.<br>
 * <br>
 * It also manages the message to notify the player that a wild pokemob has
 * decided to battle, as well as dealing with combat between rivals over a mate.
 * It is the one to queue the attack for the pokemob to perform.
 */
public class UseAttacksTask extends CombatTask implements IAICombat
{

    /** The target being attacked. */
    LivingEntity entityTarget;

    /** IPokemob version of entityTarget. */
    IPokemob pokemobTarget;

    /** Where the target is/was for attack. */
    Vector3 targetLoc = new Vector3();
    /** Move we are using */
    Move_Base attack;

    /** Temp vectors for checking things. */
    Vector3 v = new Vector3();
    Vector3 v1 = new Vector3();
    Vector3 v2 = new Vector3();

    private final float speed = 1.8f;

    /** Used for when to execute attacks. */
    protected int delayTime = -1;

    boolean waitingToStart = false;

    public UseAttacksTask(final IPokemob mob)
    {
        super(mob);
    }

    public boolean continueExecuting()
    {
        return this.pokemob.getCombatState(CombatStates.ANGRY);
    }

    @Override
    public void reset()
    {
        this.clearUseMove();
        this.waitingToStart = false;
    }

    private void setUseMove()
    {
        this.pokemob.setCombatState(CombatStates.EXECUTINGMOVE, true);
        BrainUtils.setMoveUseTarget(this.entity, this.targetLoc);
    }

    private void clearUseMove()
    {
        this.pokemob.setCombatState(CombatStates.EXECUTINGMOVE, false);
        BrainUtils.clearMoveUseTarget(this.entity);
    }

    @Override
    public void run()
    {
        // Check if the pokemob has an active move being used, if so return
        if (this.pokemob.getActiveMove() != null) return;

        this.attack = MovesUtils.getMoveFromName(this.pokemob.getMove(this.pokemob.getMoveIndex()));
        if (this.attack == null) this.attack = MovesUtils.getMoveFromName(IMoveConstants.DEFAULT_MOVE);

        if (!this.waitingToStart)
        {
            if (!((this.attack.getAttackCategory() & IMoveConstants.CATEGORY_SELF) != 0)
                    && !this.pokemob.getGeneralState(GeneralStates.CONTROLLED))
                this.setWalkTo(this.entityTarget.position(), this.speed, 0);
            this.targetLoc.set(this.entityTarget);
            this.waitingToStart = true;
            /**
             * Don't want to notify if the pokemob just broke out of a pokecube.
             */
            final boolean previousCaptureAttempt = !EntityPokecubeBase.canCaptureBasedOnConfigs(this.pokemob);

            /**
             * Check if it should notify the player of agression, and do so if
             * it should.
             */
            if (!previousCaptureAttempt && PokecubeCore.getConfig().pokemobagresswarning
                    && this.entityTarget instanceof ServerPlayer player && !(this.entityTarget instanceof FakePlayer)
                    && !this.pokemob.getGeneralState(GeneralStates.TAMED) && player.getLastHurtByMob() != this.entity
                    && player.getLastHurtMob() != this.entity)
            {
                final Component message = TComponent.translatable("pokemob.agress",
                        this.pokemob.getDisplayName().getString());
                try
                {
                    // Only send this once.
                    if (this.pokemob.getAttackCooldown() == 0) thut.lib.ChatHelper.sendSystemMessage(player, message);
                }
                catch (final Exception e)
                {
                    PokecubeAPI.LOGGER.log(Level.WARN, "Error with message for " + this.entityTarget, e);
                }
                this.pokemob.setAttackCooldown(PokecubeCore.getConfig().pokemobagressticks);
            }
            return;
        }

        // Look at the target
        BehaviorUtils.lookAtEntity(this.entity, this.entityTarget);

        // No executing move state with no target location.
        if (this.pokemob.getCombatState(CombatStates.EXECUTINGMOVE) && this.targetLoc.isEmpty()) this.clearUseMove();

        Move_Base move = null;
        move = MovesUtils.getMoveFromName(this.pokemob.getMove(this.pokemob.getMoveIndex()));
        if (move == null) move = MovesUtils.getMoveFromName(IMoveConstants.DEFAULT_MOVE);
        double var1 = (this.entity.getBbWidth() + 0.75) * (this.entity.getBbWidth() + 0.75);
        boolean distanced = false;
        final boolean self = (move.getAttackCategory() & IMoveConstants.CATEGORY_SELF) > 0;
        final double dist = this.entity.distanceToSqr(this.entityTarget.getX(), this.entityTarget.getY(),
                this.entityTarget.getZ());

        distanced = (move.getAttackCategory() & IMoveConstants.CATEGORY_DISTANCE) > 0;
        // Check to see if the move is ranged, contact or self.
        if (distanced)
            var1 = PokecubeCore.getConfig().rangedAttackDistance * PokecubeCore.getConfig().rangedAttackDistance;
        else if (PokecubeCore.getConfig().contactAttackDistance > 0)
        {
            var1 = PokecubeCore.getConfig().contactAttackDistance * PokecubeCore.getConfig().contactAttackDistance;
            distanced = true;
        }

        this.delayTime = this.pokemob.getAttackCooldown();
        final boolean canUseMove = MovesUtils.canUseMove(this.pokemob);
        if (!canUseMove) return;
        boolean shouldPath = this.delayTime <= 0;
        boolean inRange = false;

        // Checks to see if the target is in range.
        if (distanced) inRange = dist < var1;
        else inRange = MovesUtils.contactAttack(this.pokemob, this.entityTarget);

        if (self)
        {
            inRange = true;
            this.targetLoc.set(this.entity);
        }

        final boolean canSee = BrainUtils.canSee(this.entity, this.entityTarget);

        // If we have not set a move executing, we update target location. If we
        // have a move executing, we leave the old location to give the target
        // time to dodge needed.
        if (!this.pokemob.getCombatState(CombatStates.EXECUTINGMOVE))
            this.targetLoc.set(this.entityTarget).addTo(0, this.entityTarget.getBbHeight() / 2, 0);

        final boolean isTargetDodging = this.pokemobTarget != null
                && this.pokemobTarget.getCombatState(CombatStates.DODGING);

        // If the target is not trying to dodge, and the move allows it,
        // then set target location to where the target is now. This is so that
        // it can use the older postion set above, lowering the accuracy of move
        // use, allowing easier dodging.
        if (!isTargetDodging) this.targetLoc.set(this.entityTarget).addTo(0, this.entityTarget.getBbHeight() / 2, 0);

        boolean delay = false;
        // Check if the attack should, applying a new delay if this is the
        // case..
        if (inRange && canSee || self)
        {
            if (this.delayTime <= 0 && this.entity.isAddedToWorld())
            {
                this.delayTime = this.pokemob.getAttackCooldown();
                delay = true;
            }
            shouldPath = false;
            if (!self) this.setUseMove();
            else this.clearUseMove();
        }

        if (!(distanced || self))
        {
            this.setUseMove();
            BrainUtils.setLeapTarget(this.entity, new EntityTracker(this.entityTarget, false));
        }

        // If all the conditions match, queue up an attack.
        if (!this.targetLoc.isEmpty() && delay && inRange)
        {
            // Tell the target no need to try to dodge anymore, move is fired.
            if (this.pokemobTarget != null) this.pokemobTarget.setCombatState(CombatStates.DODGING, false);
            // Swing arm for effect.
            if (this.entity.getMainHandItem() != null) this.entity.swing(InteractionHand.MAIN_HAND);
            // Apply the move.
            final float f = (float) this.targetLoc.distToEntity(this.entity);
            if (this.entity.isAddedToWorld())
            {
                this.pokemob.executeMove(this.entityTarget, this.targetLoc.copy(), f);
                // Reset executing move and no item use status now that we have
                // used a move.
                this.clearUseMove();
                this.pokemob.setCombatState(CombatStates.NOITEMUSE, false);
                this.targetLoc.clear();
                shouldPath = false;
                this.delayTime = this.pokemob.getAttackCooldown();
            }
        }
        // If there is a target location, and it should path to it, queue a path
        // for the mob.
        if (!this.targetLoc.isEmpty() && shouldPath) this.setWalkTo(this.entityTarget.position(), this.speed, 0);
    }

    @Override
    public boolean shouldRun()
    {
        // If we do have the target, but are not angry, return false.
        if (!this.pokemob.getCombatState(CombatStates.ANGRY)) return false;

        final LivingEntity target = BrainUtils.getAttackTarget(this.entity);
        // No target, we can't do anything, so return false
        if (target == null) return false;
        // If either us, or target is dead, or about to be so (0 health) return
        // false
        if (!target.isAlive() || target.getHealth() <= 0 || this.pokemob.getHealth() <= 0 || !this.entity.isAlive())
            return false;

        if (target != this.entityTarget) this.pokemobTarget = PokemobCaps.getPokemobFor(target);
        this.entityTarget = target;

        return true;
    }

    @Override
    public void tick()
    {
        this.entity.getPersistentData().putLong("lastAttackTick", Tracker.instance().getTick());
    }
}
