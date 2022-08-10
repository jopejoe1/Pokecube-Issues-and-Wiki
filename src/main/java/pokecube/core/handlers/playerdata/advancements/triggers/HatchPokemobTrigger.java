package pokecube.core.handlers.playerdata.advancements.triggers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityPredicate.Composite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import pokecube.api.data.PokedexEntry;
import pokecube.api.entity.pokemob.IPokemob;
import pokecube.core.database.Database;
import pokecube.core.impl.PokecubeMod;

public class HatchPokemobTrigger implements CriterionTrigger<HatchPokemobTrigger.Instance>
{
    public static class Instance extends AbstractCriterionTriggerInstance
    {
        final PokedexEntry entry;

        public Instance(final Composite pred, final PokedexEntry entry)
        {
            super(HatchPokemobTrigger.ID, pred);
            this.entry = entry != null ? entry : Database.missingno;
        }

        public boolean test(final ServerPlayer player, final IPokemob pokemob)
        {
            return (this.entry == Database.missingno || pokemob.getPokedexEntry() == this.entry) && pokemob
                    .getOwner() == player;
        }

    }

    static class Listeners
    {
        private final PlayerAdvancements                                            playerAdvancements;
        private final Set<CriterionTrigger.Listener<HatchPokemobTrigger.Instance>> listeners = Sets.<CriterionTrigger.Listener<HatchPokemobTrigger.Instance>> newHashSet();

        public Listeners(final PlayerAdvancements playerAdvancementsIn)
        {
            this.playerAdvancements = playerAdvancementsIn;
        }

        public void add(final CriterionTrigger.Listener<HatchPokemobTrigger.Instance> listener)
        {
            this.listeners.add(listener);
        }

        public boolean isEmpty()
        {
            return this.listeners.isEmpty();
        }

        public void remove(final CriterionTrigger.Listener<HatchPokemobTrigger.Instance> listener)
        {
            this.listeners.remove(listener);
        }

        public void trigger(final ServerPlayer player, final IPokemob pokemob)
        {
            List<CriterionTrigger.Listener<HatchPokemobTrigger.Instance>> list = null;

            for (final CriterionTrigger.Listener<HatchPokemobTrigger.Instance> listener : this.listeners)
                if (listener.getTriggerInstance().test(player, pokemob))
                {
                    if (list == null)
                        list = Lists.<CriterionTrigger.Listener<HatchPokemobTrigger.Instance>> newArrayList();

                    list.add(listener);
                }
            if (list != null) for (final CriterionTrigger.Listener<HatchPokemobTrigger.Instance> listener1 : list)
                listener1.run(this.playerAdvancements);
        }
    }

    public static ResourceLocation ID = new ResourceLocation(PokecubeMod.ID, "hatch");

    private final Map<PlayerAdvancements, HatchPokemobTrigger.Listeners> listeners = Maps.<PlayerAdvancements, HatchPokemobTrigger.Listeners> newHashMap();

    public HatchPokemobTrigger()
    {
    }

    @Override
    public void addPlayerListener(final PlayerAdvancements playerAdvancementsIn,
            final CriterionTrigger.Listener<HatchPokemobTrigger.Instance> listener)
    {
        HatchPokemobTrigger.Listeners bredanimalstrigger$listeners = this.listeners.get(playerAdvancementsIn);

        if (bredanimalstrigger$listeners == null)
        {
            bredanimalstrigger$listeners = new HatchPokemobTrigger.Listeners(playerAdvancementsIn);
            this.listeners.put(playerAdvancementsIn, bredanimalstrigger$listeners);
        }

        bredanimalstrigger$listeners.add(listener);
    }

    @Override
    public Instance createInstance(final JsonObject json, final DeserializationContext conditions)
    {
        final EntityPredicate.Composite pred = EntityPredicate.Composite.fromJson(json, "player", conditions);
        final String name = json.has("entry") ? json.get("entry").getAsString() : "";
        return new HatchPokemobTrigger.Instance(pred, Database.getEntry(name));
    }

    @Override
    public ResourceLocation getId()
    {
        return HatchPokemobTrigger.ID;
    }

    @Override
    public void removePlayerListeners(final PlayerAdvancements playerAdvancementsIn)
    {
        this.listeners.remove(playerAdvancementsIn);
    }

    @Override
    public void removePlayerListener(final PlayerAdvancements playerAdvancementsIn,
            final CriterionTrigger.Listener<HatchPokemobTrigger.Instance> listener)
    {
        final HatchPokemobTrigger.Listeners bredanimalstrigger$listeners = this.listeners.get(playerAdvancementsIn);

        if (bredanimalstrigger$listeners != null)
        {
            bredanimalstrigger$listeners.remove(listener);

            if (bredanimalstrigger$listeners.isEmpty()) this.listeners.remove(playerAdvancementsIn);
        }
    }

    public void trigger(final ServerPlayer player, final IPokemob pokemob)
    {
        final HatchPokemobTrigger.Listeners bredanimalstrigger$listeners = this.listeners.get(player.getAdvancements());
        if (bredanimalstrigger$listeners != null) bredanimalstrigger$listeners.trigger(player, pokemob);
    }
}
