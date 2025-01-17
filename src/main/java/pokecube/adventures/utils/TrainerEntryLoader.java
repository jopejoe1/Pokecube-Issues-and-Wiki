package pokecube.adventures.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import pokecube.adventures.capabilities.utils.TypeTrainer;
import pokecube.api.PokecubeAPI;
import pokecube.api.data.spawns.SpawnBiomeMatcher;
import pokecube.core.database.pokedex.PokedexEntryLoader.Drop;
import pokecube.core.database.pokedex.PokedexEntryLoader.SpawnRule;
import pokecube.core.database.resources.PackFinder;
import pokecube.core.entity.npc.NpcType;
import pokecube.core.utils.Tools;
import thut.api.util.JsonUtil;
import thut.core.common.ThutCore;

public class TrainerEntryLoader
{
    public static class Bag extends Drop
    {}

    public static class Held extends Drop
    {}

    public static class TrainerEntry
    {
        String tradeTemplate = "default";
        String type;
        String pokemon;
        String gender;
        Bag bag;
        boolean belt = true;
        Held held;
        Held reward;
        Boolean replace;

        List<SpawnRule> spawns = Lists.newArrayList();
        List<String> tags = Lists.newArrayList();

        @Override
        public String toString()
        {
            return this.type + " " + this.spawns;
        }
    }

    public static class XMLDatabase
    {
        private final List<TrainerEntry> trainers = Lists.newArrayList();
        Map<String, TrainerEntry> _trainer_map = Maps.newHashMap();

        public void addEntry(final TrainerEntry entry)
        {
            final boolean replace = entry.replace != null && entry.replace;
            if (!this._trainer_map.containsKey(entry.type) || replace)
            {
                this.trainers.add(entry);
                this._trainer_map.put(entry.type, entry);
                return;
            }
        }
    }

    static XMLDatabase database;

    private static XMLDatabase loadDatabase()
    {
        final XMLDatabase full = new XMLDatabase();
        final Map<ResourceLocation, Resource> resources = PackFinder.getJsonResources(NpcType.DATALOC);
        resources.forEach((file, resource) -> {
            JsonObject loaded;
            try
            {
                final BufferedReader reader = PackFinder.getReader(file);
                if (reader == null) throw new FileNotFoundException(file.toString());
                loaded = JsonUtil.gson.fromJson(reader, JsonObject.class);
                reader.close();
                if (loaded.has("trainers"))
                {
                    final XMLDatabase database = JsonUtil.gson.fromJson(loaded, XMLDatabase.class);
                    for (final TrainerEntry entry : database.trainers)
                    {
                        if (entry.type != null) entry.type = ThutCore.trim(entry.type);
                        else continue;
                        if (entry.pokemon != null) entry.pokemon = entry.pokemon.trim();
                        if (entry.gender != null) entry.gender = entry.gender.trim();
                        full.addEntry(entry);
                    }
                }
            }
            catch (final Exception e)
            {
                PokecubeAPI.LOGGER.error("Error loading trainer database from {}", file, e);
            }
        });
        return full;
    }

    public static void makeEntries()
    {
        TrainerEntryLoader.database = TrainerEntryLoader.loadDatabase();
        for (final TrainerEntry entry : TrainerEntryLoader.database.trainers)
        {
            final String name = entry.type;
            PokecubeAPI.LOGGER.debug("Loaded Type: " + name);
            final TypeTrainer type = TypeTrainer.typeMap.containsKey(name) ? TypeTrainer.typeMap.get(name)
                    : new TypeTrainer(name);
            type.spawns.clear();
            type.pokemon.clear();
            final byte male = 1;
            final byte female = 2;
            type.tradeTemplate = entry.tradeTemplate;
            type.hasBag = entry.bag != null;
            if (entry.tags != null) entry.tags.forEach(s -> type.tags.add(new ResourceLocation(s)));
            if (type.hasBag)
            {
                final ItemStack bag = Tools.getStack(entry.bag.getValues());
                type.bag = bag;
            }
            if (entry.spawns != null) entry.spawns.forEach(rule -> {
                Float weight;
                try
                {
                    weight = Float.parseFloat(rule.values.remove("rate"));
                }
                catch (final Exception e)
                {
                    PokecubeAPI.LOGGER.warn(
                            "Error with weight for " + type.getName() + " " + rule.values + " " + entry.spawns, e);
                    return;
                }
                final SpawnBiomeMatcher matcher = SpawnBiomeMatcher.get(rule);
                type.spawns.put(matcher, weight);
            });
            type.hasBelt = entry.belt;
            if (entry.gender != null) type.genders = entry.gender.equalsIgnoreCase("male") ? male
                    : entry.gender.equalsIgnoreCase("female") ? female : male + female;
            if (entry.held != null)
            {
                final ItemStack held = Tools.getStack(entry.held.getValues());
                type.held = held;
            }
            type.pokelist = entry.pokemon == null ? new String[] {} : entry.pokemon.split(",");
        }
    }
}
