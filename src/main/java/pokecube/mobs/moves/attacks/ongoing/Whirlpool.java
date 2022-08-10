package pokecube.mobs.moves.attacks.ongoing;

import java.util.Random;

import net.minecraft.world.entity.LivingEntity;
import pokecube.api.entity.IOngoingAffected;
import pokecube.api.entity.IOngoingAffected.IOngoingEffect;
import pokecube.api.entity.pokemob.IPokemob;
import pokecube.core.impl.capabilities.CapabilityPokemob;
import pokecube.core.moves.templates.Move_Ongoing;
import pokecube.core.utils.PokeType;
import thut.core.common.ThutCore;

public class Whirlpool extends Move_Ongoing
{

    public Whirlpool()
    {
        super("whirlpool");
    }

    @Override
    public void doOngoingEffect(final LivingEntity user, final IOngoingAffected mob, final IOngoingEffect effect)
    {
        final IPokemob pokemob = CapabilityPokemob.getPokemobFor(mob.getEntity());
        if (pokemob != null && pokemob.isType(PokeType.getType("ghost"))) return;
        super.doOngoingEffect(user, mob, effect);
    }

    @Override
    public int getDuration()
    {
        final Random r = ThutCore.newRandom();
        return 2 + r.nextInt(4);
    }

}
