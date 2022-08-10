package pokecube.mobs.abilities.i;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import pokecube.api.entity.pokemob.IPokemob;
import pokecube.api.entity.pokemob.moves.MovePacket;
import pokecube.api.moves.IMoveConstants;
import pokecube.core.database.abilities.Ability;

public class IronBarbs extends Ability
{
    @Override
    public int beforeDamage(IPokemob mob, MovePacket move, int damage)
    {
        if ((move.getMove().getAttackCategory() & IMoveConstants.CATEGORY_CONTACT) > 0)
        {
            final LivingEntity entity = move.attacker.getEntity();
            final float maxHp = entity.getMaxHealth();
            // TODO message about recoil
            entity.hurt(DamageSource.MAGIC, 0.125f * maxHp);
        }
        return damage;
    }
}
