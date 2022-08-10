package pokecube.mobs.abilities.l;

import pokecube.api.entity.pokemob.IPokemob;
import pokecube.api.entity.pokemob.moves.MovePacket;
import pokecube.api.moves.IMoveConstants;
import pokecube.core.database.abilities.Ability;

public class Limber extends Ability
{
    @Override
    public void onMoveUse(IPokemob mob, MovePacket move)
    {
        final IPokemob attacker = move.attacker;
        if (attacker == mob || !move.pre || attacker == move.attacked) return;
        if (move.statusChange == IMoveConstants.STATUS_PAR) move.statusChange = IMoveConstants.STATUS_NON;
    }

    @Override
    public void onUpdate(IPokemob mob)
    {
        if (mob.getStatus() == IMoveConstants.STATUS_PAR) mob.healStatus();
    }

}
