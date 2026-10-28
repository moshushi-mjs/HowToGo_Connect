package io.github.moshushimjs.htgconnect.client;

import java.util.List;

import io.github.moshushimjs.htgconnect.HowToGoConnect;
import io.github.moshushimjs.htgconnect.net.DeclineRoadPayload;
import io.github.moshushimjs.htgconnect.net.Dimensions;
import io.github.moshushimjs.htgconnect.net.RequestRoadPayload;
import io.github.moshushimjs.htgconnect.net.RoadOfferPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class RoadConsentScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 210;
    private static final int ROW_HEIGHT = 12;
    private static final int LIST_WIDTH = 250;
    private static final int LIST_HEIGHT = 78;
    private static final int LIST_PADDING = 4;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_GAP = 4;
    private static final int SCROLLBAR_GRAB = 3;
    private static final int TEXT_GAP = 4;
    private static final int MIN_THUMB = 10;
    private static final int DOT_COLOR = 0xFF4A4A4A;
    private static final int NAME_COLOR = 0xFFFFFF;
    private static final int SIZE_COLOR = 0xFFD700;
    private static final int TRACK_COLOR = 0xFF333333;
    private static final int THUMB_COLOR = 0xFF55CCFF;
    private static final int THUMB_ACTIVE_COLOR = 0xFF9EE6FF;

    private final List<RoadOfferPayload.Entry> entries;
    private final long totalBytes;
    private final boolean kickOnDecline;

    private double scroll = 0.0;
    private boolean accepted = false;
    private boolean handled = false;
    private boolean focusCleared = false;
    private boolean dragging = false;
    private double dragOffset = 0.0;

    public RoadConsentScreen(List<RoadOfferPayload.Entry> entries, long totalBytes, boolean kickOnDecline) {
        super(Component.translatable("htg_connect.consent.title"));
        this.entries = entries;
        this.totalBytes = totalBytes;
        this.kickOnDecline = kickOnDecline;
        HowToGoConnect.LOGGER.info("弹出路网接收界面：{} 个维度，共 {} 字节，拒绝后{}",
                entries.size(), totalBytes, kickOnDecline ? "踢出" : "只提示");
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private int listLeft() {
        return panelLeft() + (PANEL_WIDTH - LIST_WIDTH) / 2;
    }

    private int listTop() {
        return panelTop() + 52;
    }

    private int visibleHeight() {
        return LIST_HEIGHT - LIST_PADDING * 2;
    }

    private int maxScroll() {
        return Math.max(0, this.entries.size() * ROW_HEIGHT - visibleHeight());
    }

    private boolean scrollable() {
        return maxScroll() > 0;
    }

    private int textRight() {
        return listLeft() + LIST_WIDTH - (scrollable() ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
    }

    private int trackLeft() {
        return listLeft() + LIST_WIDTH - SCROLLBAR_GAP - SCROLLBAR_WIDTH;
    }

    private int trackTop() {
        return listTop() + LIST_PADDING;
    }

    private int trackHeight() {
        return LIST_HEIGHT - LIST_PADDING * 2;
    }

    private int thumbHeight() {
        int content = this.entries.size() * ROW_HEIGHT;
        if (content <= 0) {
            return trackHeight();
        }
        int height = trackHeight() * visibleHeight() / content;
        return Math.min(trackHeight(), Math.max(MIN_THUMB, height));
    }

    private int thumbTop() {
        int height = thumbHeight();
        if (height >= trackHeight()) {
            return trackTop();
        }
        int max = maxScroll();
        if (max <= 0) {
            return trackTop();
        }
        return trackTop() + (int) ((trackHeight() - height) * (this.scroll / max));
    }

    private boolean overTrack(double mouseX, double mouseY) {
        if (!scrollable()) {
            return false;
        }
        return mouseX >= trackLeft() - SCROLLBAR_GRAB
                && mouseX <= trackLeft() + SCROLLBAR_WIDTH + SCROLLBAR_GRAB
                && mouseY >= trackTop()
                && mouseY <= trackTop() + trackHeight();
    }

    private void scrollToThumb(int thumbTop) {
        int height = thumbHeight();
        int span = trackHeight() - height;
        if (span <= 0) {
            this.scroll = 0.0;
            return;
        }
        int clamped = Math.max(trackTop(), Math.min(trackTop() + span, thumbTop));
        this.scroll = (double) (clamped - trackTop()) / span * maxScroll();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = panelTop() + 182;

        this.addRenderableWidget(Button.builder(
                Component.translatable("htg_connect.consent.accept"), button -> {
                    this.accepted = true;
                    this.handled = true;
                    ClientRoads.consent();
                    requestCurrent();
                    this.onClose();
                }).bounds(centerX - 105, buttonY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("htg_connect.consent.decline"), button -> {
                    this.handled = true;
                    ClientRoads.decline();
                    if (this.kickOnDecline) {
                        ClientRoads.forget();
                        PacketDistributor.sendToServer(new DeclineRoadPayload());
                    } else {
                        Minecraft.getInstance().setScreen(new RoadDeclinedScreen());
                    }
                }).bounds(centerX + 5, buttonY, 100, 20).build());

        this.clearFocus();
    }

    private void requestCurrent() {
        String current = ClientRoads.currentDimension();
        if (current == null) {
            return;
        }
        for (RoadOfferPayload.Entry entry : this.entries) {
            if (Dimensions.same(entry.dimension(), current)) {
                PacketDistributor.sendToServer(new RequestRoadPayload(entry.dimension(), 0));
                return;
            }
        }
        HowToGoConnect.LOGGER.info("同意接收，但服务端在 {} 没有路网", current);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overTrack(mouseX, mouseY)) {
            int top = thumbTop();
            int height = thumbHeight();
            if (mouseY >= top && mouseY <= top + height) {
                this.dragOffset = mouseY - top;
            } else {
                this.dragOffset = height / 2.0;
                scrollToThumb((int) mouseY - (int) this.dragOffset);
            }
            this.dragging = true;
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        this.clearFocus();
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging) {
            scrollToThumb((int) (mouseY - this.dragOffset));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging) {
            this.dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScroll();
        if (max <= 0) {
            return false;
        }
        this.scroll -= scrollY * ROW_HEIGHT;
        if (this.scroll < 0) {
            this.scroll = 0;
        }
        if (this.scroll > max) {
            this.scroll = max;
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.kickOnDecline && !this.handled && !this.accepted) {
            ClientRoads.forget();
            PacketDistributor.sendToServer(new DeclineRoadPayload());
        }
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.focusCleared) {
            this.focusCleared = true;
            this.clearFocus();
        }
        graphics.fill(0, 0, this.width, this.height, 0xB0000000);

        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left - 2, top - 2, left + PANEL_WIDTH + 2, top + PANEL_HEIGHT + 2, 0xFF55CCFF);
        graphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, 0xFF0A0A0A);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF141414);

        int centerX = this.width / 2;

        graphics.drawCenteredString(this.font, this.title, centerX, top + 12, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("htg_connect.consent.body"), centerX, top + 32, 0xFFFFFF);

        drawList(graphics);
        drawScrollbar(graphics);

        graphics.drawCenteredString(this.font,
                Component.translatable("htg_connect.consent.total", sizeText(this.totalBytes)),
                centerX, top + 138, SIZE_COLOR);
        graphics.drawCenteredString(this.font,
                Component.translatable("htg_connect.consent.hint"), centerX, top + 154, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("htg_connect.consent.note"), centerX, top + 168, 0xBBBBBB);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawList(GuiGraphics graphics) {
        int left = listLeft();
        int top = listTop();
        int right = left + LIST_WIDTH;

        graphics.fill(left - 1, top - 1, right + 1, top + LIST_HEIGHT + 1, 0xFF3C7A94);
        graphics.fill(left, top, right, top + LIST_HEIGHT, 0xFF1B1B1B);

        graphics.enableScissor(left, top, right, top + LIST_HEIGHT);

        int index = 0;
        for (RoadOfferPayload.Entry entry : this.entries) {
            int rowY = top + LIST_PADDING + (int) (index * ROW_HEIGHT - this.scroll);
            index++;
            if (rowY + ROW_HEIGHT <= top + LIST_PADDING || rowY >= top + LIST_HEIGHT - LIST_PADDING) {
                continue;
            }

            Component name = Dimensions.name(entry.dimension());
            String size = sizeText(entry.bytes());
            int sizeWidth = this.font.width(size);

            int available = textRight() - (left + LIST_PADDING) - sizeWidth - TEXT_GAP * 2;
            String nameText = name.getString();
            if (this.font.width(nameText) > available) {
                nameText = this.font.plainSubstrByWidth(nameText, Math.max(8, available - 7)) + "…";
            }
            int nameWidth = this.font.width(nameText);

            graphics.drawString(this.font, nameText, left + LIST_PADDING, rowY, NAME_COLOR, false);
            graphics.drawString(this.font, size, textRight() - sizeWidth, rowY, SIZE_COLOR, false);

            int dotsFrom = left + LIST_PADDING + nameWidth + TEXT_GAP;
            int dotsTo = textRight() - sizeWidth - TEXT_GAP;
            for (int dotX = dotsFrom; dotX < dotsTo; dotX += 3) {
                graphics.fill(dotX, rowY + 4, dotX + 1, rowY + 5, DOT_COLOR);
            }
        }

        graphics.disableScissor();
    }

    private void drawScrollbar(GuiGraphics graphics) {
        if (!scrollable()) {
            return;
        }
        int trackX = trackLeft();
        int top = trackTop();
        int height = trackHeight();
        graphics.fill(trackX, top, trackX + SCROLLBAR_WIDTH, top + height, TRACK_COLOR);

        int thumbHeight = thumbHeight();
        int thumbTop = thumbTop();
        graphics.fill(trackX, thumbTop, trackX + SCROLLBAR_WIDTH, thumbTop + thumbHeight,
                this.dragging ? THUMB_ACTIVE_COLOR : THUMB_COLOR);
    }

    private String sizeText(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
        if (bytes >= 1024L) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
