package victorgponce.com.webdataupdater;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import victorgponce.com.webdataupdater.network.Uploader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Webdataupdater.MODID)
public class Webdataupdater {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "webdataupdater";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "webdataupdater" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "webdataupdater" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // Creates a new Block with the id "webdataupdater:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of(Material.STONE)));
    // Creates a new BlockItem with the id "webdataupdater:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

    private static final String FILE_NAME = "server_metrics.json";
    private static final long INTERVAL_TICKS = 30 * 20;
    private long lastTickTime = 0;

    private static final String SERVER_URL = "";

    public Webdataupdater() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        System.out.println("Mod ServerMetrics inicializado correctamente.");
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level.isClientSide) return;

        // Solo contar las muertes de jugadores
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer) {
            // Actualizamos las muertes
            int totalDeaths = getTotalDeaths();
            totalDeaths++;
            saveMetric("totalDeaths", totalDeaths);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        long currentTime = server.getTickCount();

        if (currentTime - lastTickTime >= INTERVAL_TICKS) {
            lastTickTime = currentTime;
            saveMetrics(server);
        }
    }

    private void saveMetrics(MinecraftServer server) {
        // Jugadores en línea
        int onlinePlayers = server.getPlayerList().getPlayerCount();

        // Días Jugados del Servidor
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        long daysPlayed = overworld != null ? overworld.getDayTime() / 24000 : 0;

        // Guardamos las métricas
        JsonObject metrics = new JsonObject();
        metrics.addProperty("onlinePlayers", onlinePlayers);
        metrics.addProperty("totalDeaths", getTotalDeaths());
        metrics.addProperty("daysPlayed", daysPlayed);

        saveToFile(metrics);

        // Subir el archivo JSON al servidor
        Uploader.postMethod(FILE_NAME, SERVER_URL);
    }

    private void saveToFile(JsonObject metrics) {
        File file = new File(FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new Gson();
            gson.toJson(metrics, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getTotalDeaths() {
        JsonObject json = loadFromFile();
        return json != null && json.has("totalDeaths") ? json.get("totalDeaths").getAsInt() : 0;
    }

    private void saveMetric(String key, int value) {
        JsonObject json = loadFromFile();
        if (json == null) {
            json = new JsonObject();
        }
        json.addProperty(key, value);
        saveToFile(json);
    }

    private JsonObject loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return new JsonObject();  // Si no existe el archivo, retornamos un JSON vacío
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
