package pokecube.legends.worldgen;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ColumnFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DeltaFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pokecube.legends.Reference;
import pokecube.legends.worldgen.features.BasaltColumnsFeature;
import pokecube.legends.worldgen.features.CrystallizedSandstoneBouldersFeature;
import pokecube.legends.worldgen.features.DeadCoralClawFeature;
import pokecube.legends.worldgen.features.DeadCoralMushroomFeature;
import pokecube.legends.worldgen.features.DeadCoralTreeFeature;
import pokecube.legends.worldgen.features.DeltaFeature;
import pokecube.legends.worldgen.features.DesertRockFeature;
import pokecube.legends.worldgen.features.DiskBaseFeature;
import pokecube.legends.worldgen.features.DiskFeature;
import pokecube.legends.worldgen.features.DistortedIslandsFeature;
import pokecube.legends.worldgen.features.DistorticStoneBouldersFeature;
import pokecube.legends.worldgen.features.DistorticVinesFeature;
import pokecube.legends.worldgen.features.ForestVegetationFeature;
import pokecube.legends.worldgen.features.LakeFeature;
import pokecube.legends.worldgen.features.StringOfPearlsFeature;
import pokecube.legends.worldgen.features.TaintedKelpFeature;
import pokecube.legends.worldgen.features.TaintedSeagrassFeature;
import pokecube.legends.worldgen.features.TemporalBambooFeature;
import pokecube.legends.worldgen.trees.treedecorators.LeavesStringOfPearlsDecorator;
import pokecube.legends.worldgen.trees.treedecorators.TrunkStringOfPearlsDecorator;

public class WorldgenFeatures
{
//    public static final DeferredRegister<SurfaceBuilder<?>> SURFACE_BUILDERS = DeferredRegister.create(
//            ForgeRegistries.SURFACE_BUILDERS, Reference.ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(
            ForgeRegistries.FEATURES, Reference.ID);

//    public static final RegistryObject<SurfaceBuilder<?>> BURNT_DESERT = WorldgenFeatures.SURFACE_BUILDERS.register("burnt_desert_builder",
//            () -> new BurntDesertSurfaceBuilder(SurfaceBuilderBaseConfiguration.CODEC));
//    public static final RegistryObject<SurfaceBuilder<?>> MIRAGE_DESERT = WorldgenFeatures.SURFACE_BUILDERS.register("mirage_desert_builder",
//            () -> new MirageDesertSurfaceBuilder(SurfaceBuilderBaseConfiguration.CODEC));
//    public static final RegistryObject<SurfaceBuilder<?>> BLINDING_DELTAS = WorldgenFeatures.SURFACE_BUILDERS.register("blinding_deltas_builder",
//            () -> new BlindingDeltasSurfaceBuilder(SurfaceBuilderBaseConfiguration.CODEC));
//    public static final RegistryObject<SurfaceBuilder<?>> TAINTED_BARRENS = WorldgenFeatures.SURFACE_BUILDERS.register("tainted_barrens_builder",
//            () -> new TaintedBarrensSurfaceBuilder(SurfaceBuilderBaseConfiguration.CODEC));

    public static final RegistryObject<Feature<?>> BASALT_COLUMNS = WorldgenFeatures.FEATURES.register("basalt_columns_feature",
            () -> new BasaltColumnsFeature(ColumnFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> CRYS_SANDSTONE_BOULDERS = WorldgenFeatures.FEATURES.register("crystallized_sandstone_boulders_feature",
            () -> new CrystallizedSandstoneBouldersFeature(ColumnFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> DEAD_CORAL_CLAW = WorldgenFeatures.FEATURES.register("dead_coral_claw_feature",
            () -> new DeadCoralClawFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> DEAD_CORAL_MUSHROOM = WorldgenFeatures.FEATURES.register("dead_coral_mushroom_feature",
            () -> new DeadCoralMushroomFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> DEAD_CORAL_TREE = WorldgenFeatures.FEATURES.register("dead_coral_tree_feature",
            () -> new DeadCoralTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> DELTA = WorldgenFeatures.FEATURES.register("delta_feature",
            () -> new DeltaFeature(DeltaFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> DESERT_ROCK = WorldgenFeatures.FEATURES.register("desert_rock_feature",
            () -> new DesertRockFeature(BlockStateConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> DISK = WorldgenFeatures.FEATURES.register("disk_feature",
            () -> new DiskFeature(DiskConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> DISK_BASE = WorldgenFeatures.FEATURES.register("disk_base_feature",
            () -> new DiskBaseFeature(DiskConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> DISTORTED_ISLANDS = WorldgenFeatures.FEATURES.register("distorted_islands_feature",
            () -> new DistortedIslandsFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> DISTORTIC_STONE_BOULDERS = WorldgenFeatures.FEATURES.register("distortic_stone_boulders_feature",
            () -> new DistorticStoneBouldersFeature(ColumnFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> DISTORTIC_VINES = WorldgenFeatures.FEATURES.register("distortic_vines_feature",
            () -> new DistorticVinesFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> FORBIDDEN_TAIGA_VEGETATION = WorldgenFeatures.FEATURES.register("forsaken_taiga_vegetation_feature",
            () -> new ForestVegetationFeature(BlockPileConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> LAKE = WorldgenFeatures.FEATURES.register("lake_feature",
            () -> new LakeFeature(BlockStateConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> TEMPORAL_BAMBOO = WorldgenFeatures.FEATURES.register("temporal_bamboo_feature",
            () -> new TemporalBambooFeature(ProbabilityFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> TAINTED_KELP= WorldgenFeatures.FEATURES.register("tainted_kelp_feature",
            () -> new TaintedKelpFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> TAINTED_SEAGRASS = WorldgenFeatures.FEATURES.register("tainted_seagrass_feature",
            () -> new TaintedSeagrassFeature(ProbabilityFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<?>> STRING_OF_PEARLS = WorldgenFeatures.FEATURES.register("string_of_pearls_feature",
            () -> new StringOfPearlsFeature(NoneFeatureConfiguration.CODEC));


    public static void init(final IEventBus bus)
    {
//        WorldgenFeatures.SURFACE_BUILDERS.register(bus);
        WorldgenFeatures.FEATURES.register(bus);
    }
}