package victorgponce.com.webdataupdater;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import victorgponce.com.webdataupdater.network.Uploader; // Importamos la clase Uploader

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static victorgponce.com.webdataupdater.Webdataupdater.MODID;

@Mod(MODID)
public class ServerMetrics {

    private static final String FILE_NAME = "server_metrics.json";
    private static final long INTERVAL_TICKS = 30 * 20;
    private long lastTickTime = 0;

    private static final String SERVER_URL = "";

    public ServerMetrics() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
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
