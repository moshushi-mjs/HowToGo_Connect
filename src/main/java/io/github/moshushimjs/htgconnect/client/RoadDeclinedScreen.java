package io.github.moshushimjs.htgconnect.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RoadDeclinedScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 100;

    public RoadDeclinedScreen() {
        super(Component.translatable("htg_connect.consent.title"));
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(
                Component.translatable("htg_connect.declined.ok"), button -> this.onClose())
                .bounds(this.width / 2 - 50, panelTop() + 66, 100, 20)
                .build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xB0000000);

        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, 0xFF55CCFF);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF141414);

        int centerX = this.width / 2;

        graphics.drawCenteredString(this.font,
                Component.translatable("htg_connect.declined.body"), centerX, top + 30, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
