package com.lanterns;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class LanternsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DisguisePayload.ID,
                (payload, context) -> context.client().execute(() -> ClientDisguises.apply(payload)));

        ClientPlayNetworking.registerGlobalReceiver(OpenTaskPayload.ID, (payload, context) -> context.client().execute(() -> {
            MinecraftClient mc = context.client();
            if (payload.ui().equals("close")) {
                if (mc.currentScreen instanceof TaskScreen) mc.setScreen(null);
                return;
            }
            mc.setScreen(new TaskScreen(payload.taskIndex(), payload.ui(), payload.seed(), payload.title()));
        }));

        ClientPlayNetworking.registerGlobalReceiver(TaskListPayload.ID,
                (payload, context) -> context.client().execute(() -> TaskHud.apply(payload)));

        ClientPlayNetworking.registerGlobalReceiver(VoteStatePayload.ID, (payload, context) -> context.client().execute(() -> {
            MinecraftClient mc = context.client();
            if (payload.data().isEmpty()) { if (mc.currentScreen instanceof VoteScreen) mc.setScreen(null); return; }
            if (mc.currentScreen instanceof VoteScreen vs) vs.update(payload);
            else if (payload.open()) mc.setScreen(new VoteScreen(payload)); // updates never steal focus back after ESC
        }));

        ClientPlayNetworking.registerGlobalReceiver(PickPlayerPayload.ID,
                (payload, context) -> context.client().execute(() -> context.client().setScreen(new PickScreen(payload))));

        // Ability key (default G): the server decides what it opens for your role, if anything.
        KeyBinding abilityKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.lanterns.ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.lanterns"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (abilityKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null && ClientPlayNetworking.canSend(AbilityKeyPayload.ID))
                    ClientPlayNetworking.send(new AbilityKeyPayload());
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> TaskHud.render(ctx));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { ClientDisguises.clear(); TaskHud.clear(); });
    }
}
