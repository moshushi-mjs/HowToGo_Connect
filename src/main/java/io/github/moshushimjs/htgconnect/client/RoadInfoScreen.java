package io.github.moshushimjs.htgconnect.client;

import io.github.moshushimjs.htgconnect.net.Dimensions;
import io.github.moshushimjs.htgconnect.net.RequestCatalogPayload;
import io.github.moshushimjs.htgconnect.net.RequestRoadPayload;
import io.github.moshushimjs.htgconnect.net.RoadOfferPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class RoadInfoScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 210;
    private static final int LIST_WIDTH = 250;
    private static final int LIST_HEIGHT = 78;
    private static final int LIST_PADDING = 4;
    private static final int ROW_HEIGHT = 12;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_GAP = 4;
    private static final int SCROLLBAR_GRAB = 3;
    private static final int MIN_THUMB = 10;
    private static final int TEXT_GAP = 3;
    private static final int DOTS_COLOR = 0xFF4A4A4A;
    private static final int NAME_COLOR = 0xFFFFFF;
    private static final int SIZE_COLOR = 0xFFD700;
    private static final int STATUS_ACCEPTED = 0xFF55FF55;
    private static final int STATUS_OUTDATED = 0xFFFFD700;
    private static final int STATUS_NONE = 0xFF999999;
    private static final int TRACK_COLOR = 0xFF333333;
    private static final int THUMB_COLOR = 0xFF55CCFF;
    private static final int THUMB_ACTIVE_COLOR = 0xFF9EE6FF;
    private static final int BORDER_COLOR = 0xFF3C7A94;
    private static final int LIST_BG = 0xFF1B1B1B;


    private Button acceptButton;
    private Button refreshButton;

    private double scroll = 0.0;
    private boolean focusCleared = false;
    private boolean dragging = false;
    private double dragOffset = 0.0;
    private boolean asked = false;

    public RoadInfoScreen() {
        super(Component.translatable("htg_connect.info.title"));
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
        return panelTop() + 50;
    }

    private int visibleHeight() {
        return LIST_HEIGHT - LIST_PADDING * 2;
    }

    private List<RoadOfferPayload.Entry> entries() {
        return ServerCatalog.entries();
    }

    private int maxScroll() {
        return Math.max(0, entries().size() * ROW_HEIGHT - visibleHeight());
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
        int content = entries().size() * ROW_HEIGHT;
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

    private void scrollToThumb(int top) {
        int height = thumbHeight();
        int span = trackHeight() - height;
        if (span <= 0) {
            this.scroll = 0.0;
            return;
        }
        int clamped = Math.max(trackTop(), Math.min(trackTop() + span, top));
        this.scroll = (double) (clamped - trackTop()) / span * maxScroll();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = panelTop() + 180;

        this.acceptButton = this.addRenderableWidget(Button.builder(
                Component.translatable("htg_connect.info.accept"), button -> accept())
                .bounds(centerX - 145, buttonY, 90, 20).build());
        this.acceptButton.active = !ClientRoads.consented() && !ClientRoads.isHost();

        this.refreshButton = this.addRenderableWidget(Button.builder(
                Component.translatable("htg_connect.info.refresh"), button -> refresh())
                .bounds(centerX - 45, buttonY, 90, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("htg_connect.info.close"), button -> this.onClose())
                .bounds(centerX + 55, buttonY, 90, 20).build());

        if (!ServerCatalog.received() && !this.asked) {
            this.asked = true;
            PacketDistributor.sendToServer(new RequestCatalogPayload());
        }

        this.clearFocus();
    }

    private void accept() {
        if (ClientRoads.consented() || ClientRoads.isHost()) {
            return;
        }
        ClientRoads.consent();
        String dimension = ClientRoads.currentDimension();
        if (dimension != null) {
            PacketDistributor.sendToServer(new RequestRoadPayload(dimension, 0));
        }
        this.rebuildWidgets();
    }

    private void refresh() {
        PacketDistributor.sendToServer(new RequestCatalogPayload());
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

        Component subtitle = ServerCatalog.received()
                ? Component.translatable("htg_connect.info.subtitle", entries().size())
                : Component.translatable("htg_connect.info.waiting");
        graphics.drawCenteredString(this.font, subtitle, centerX, top + 32, 0xBBBBBB);

        drawList(graphics);
        drawScrollbar(graphics);

        graphics.drawCenteredString(this.font,
                Component.translatable("htg_connect.info.total", sizeText(ServerCatalog.totalBytes())),
                centerX, top + 138, SIZE_COLOR);
        String[] stateLines = stateText().getString().split("\n");
        int stateY = top + 156 - (stateLines.length - 1) * 5;
        for (String line : stateLines) {
            graphics.drawCenteredString(this.font, line, centerX, stateY, stateColor());
            stateY += 11;
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (mouseX >= this.width / 2 - 145 && mouseX <= this.width / 2 - 55
                && mouseY >= top + 180 && mouseY <= top + 200
                && (ClientRoads.consented() || ClientRoads.isHost())) {
            graphics.renderTooltip(this.font,
                    Component.translatable("htg_connect.info.already"),
                    (int) mouseX, (int) mouseY);
        }
    }

    private Component stateText() {
        if (ClientRoads.isHost()) {
            return Component.translatable("htg_connect.info.state.host");
        }
        if (ClientRoads.consented()) {
            return Component.translatable("htg_connect.info.state.consented");
        }
        if (ClientRoads.decided()) {
            return Component.translatable("htg_connect.info.state.declined");
        }
        return Component.translatable("htg_connect.info.state.undecided");
    }

    private int stateColor() {
        if (ClientRoads.isHost()) {
            return STATUS_NONE;
        }
        return ClientRoads.consented() ? STATUS_ACCEPTED : STATUS_NONE;
    }

    private void drawList(GuiGraphics graphics) {
        int left = listLeft();
        int top = listTop();
        int right = left + LIST_WIDTH;

        graphics.fill(left - 1, top - 1, right + 1, top + LIST_HEIGHT + 1, BORDER_COLOR);
        graphics.fill(left, top, right, top + LIST_HEIGHT, LIST_BG);

        int total = ServerCatalog.received() ? entries().size() : 0;
        if (total == 0) {
            String message = ServerCatalog.received()
                    ? Component.translatable("htg_connect.info.empty").getString()
                    : Component.translatable("htg_connect.info.waiting").getString();
            graphics.drawCenteredString(this.font, message, left + LIST_WIDTH / 2,
                    top + LIST_HEIGHT / 2 - 4, 0x888888);
            return;
        }

        graphics.enableScissor(left, top, right, top + LIST_HEIGHT);

        int index = 0;
        for (RoadOfferPayload.Entry entry : entries()) {
            int rowY = top + LIST_PADDING + (int) (index * ROW_HEIGHT - this.scroll);
            index++;
            if (rowY + ROW_HEIGHT <= top + LIST_PADDING || rowY >= top + LIST_HEIGHT - LIST_PADDING) {
                continue;
            }

            Component name = Dimensions.name(entry.dimension());
            String size = sizeText(entry.bytes());
            Component status = statusText(entry.dimension(), entry.version());
            int statusColor = statusColor(entry.dimension(), entry.version());

            int statusWidth = this.font.width(status);
            int sizeWidth = this.font.width(size);

            int statusX = textRight() - statusWidth;
            int sizeX = statusX - 6 - sizeWidth;

            String nameText = name.getString();
            int available = sizeX - (left + LIST_PADDING) - TEXT_GAP;
            if (this.font.width(nameText) > available) {
                nameText = this.font.plainSubstrByWidth(nameText, Math.max(8, available - 7)) + "…";
            }
            int nameWidth = this.font.width(nameText);

            graphics.drawString(this.font, nameText, left + LIST_PADDING, rowY, NAME_COLOR, false);
            graphics.drawString(this.font, size, sizeX, rowY, SIZE_COLOR, false);
            graphics.drawString(this.font, status, statusX, rowY, statusColor, false);

            int dotsFrom = left + LIST_PADDING + nameWidth + TEXT_GAP;
            int dotsTo = sizeX - TEXT_GAP;
            for (int dotX = dotsFrom; dotX < dotsTo; dotX += 3) {
                graphics.fill(dotX, rowY + 4, dotX + 1, rowY + 5, DOTS_COLOR);
            }
        }

        graphics.disableScissor();
    }

    private Component statusText(String dimension, int version) {
        if (!ClientRoads.consented()) {
            return Component.translatable("htg_connect.info.status.none");
        }
        int known = ClientRoads.versionOf(dimension);
        if (known == 0) {
            return Component.translatable("htg_connect.info.status.none");
        }
        if (known == version) {
            return Component.translatable("htg_connect.info.status.accepted");
        }
        return Component.translatable("htg_connect.info.status.outdated");
    }

    private int statusColor(String dimension, int version) {
        if (!ClientRoads.consented()) {
            return STATUS_NONE;
        }
        int known = ClientRoads.versionOf(dimension);
        if (known == 0) {
            return STATUS_NONE;
        }
        return known == version ? STATUS_ACCEPTED : STATUS_OUTDATED;
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
