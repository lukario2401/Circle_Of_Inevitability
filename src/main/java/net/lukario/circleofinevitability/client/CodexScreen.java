package net.lukario.circleofinevitability.client;
import java.util.*;
import net.lukario.circleofinevitability.combat.AbilityStudy.Row;
import net.lukario.circleofinevitability.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Scrollable list: left click selects; the star toggles favorites without selecting. */
public final class CodexScreen extends Screen {
    private static CodexScreen active;
    private final List<Row> rows;
    private final Map<String,Optional<Identifier>> icons=new HashMap<>();
    private int scroll;
    private CodexScreen(List<Row> rows,int scroll) { super(Component.literal("Studied abilities"));this.rows=rows;this.scroll=scroll; }
    public static void open(List<Row> rows) {
        var mc=Minecraft.getInstance();
        int offset=active==null?0:active.scroll;
        CodexScreen next=new CodexScreen(rows,offset);
        mc.gui.setScreen(next);
        active=next;
    }
    @Override public void removed(){if(active==this)active=null;super.removed();}
    private int left(){return (width-panelWidth())/2;}
    private int panelWidth(){return Math.min(520,width-24);}
    private int top(){return 24;}
    private int visible(){return Math.max(1,(height-100)/28);}
    private void clamp(){scroll=Math.clamp(scroll,0,Math.max(0,rows.size()-visible()));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){scroll-= (int)Math.signum(dy)*3;clamp();return true;}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice) {
        if(e.button()!=0)return super.mouseClicked(e,twice);
        int local=(int)e.y()-(top()+30);
        int index=scroll+local/28;
        if(e.x()>=left()+8 && e.x()<left()+panelWidth()-8 && local>=0 && local<visible()*28 && index<rows.size()) {
            boolean favorite=e.x()>=left()+panelWidth()-34;
            if(!favorite && (rows.get(index).study()<100 || rows.get(index).sequence()<ClientPathwayData.getSequence()))return true;
            PacketHandler.sendToServer(new SCodexChoicePacket(rows.get(index).id(),favorite));
            if(!favorite)onClose();
            return true;
        }
        return super.mouseClicked(e,twice);
    }
    private Optional<Identifier> icon(Row row) {
        return icons.computeIfAbsent(row.icon(),s->{
            for(String candidate:new String[]{s,s.replace("ability_menus/",""),s.replace("dark_angel/","hanged_man/").replace("ability_menus/","")}) {
                Identifier id=Identifier.fromNamespaceAndPath("circle_of_inevitability","textures/gui/"+candidate+".png");
                if(minecraft.getResourceManager().getResource(id).isPresent())return Optional.of(id);
            }
            return Optional.empty();
        });
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float tick){minecraft.gui.hud.extractDeferredSubtitles();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick) {
        clamp(); int x=left(),y=top(),w=panelWidth(),bottom=y+30+visible()*28;
        g.fillGradient(0,0,width,height,0x88000000,0xAA000000);
        g.fill(x,y,x+w,bottom+24,0xF21C1820);
        g.fill(x,y,x+w,y+2,0xFFD1A94B);
        g.text(font,title,x+12,y+10,0xFFF4EBD0);
        for(int n=0;n<visible() && scroll+n<rows.size();n++) {
            Row row=rows.get(scroll+n); int yy=y+30+n*28;
            boolean hover=mx>=x+8 && mx<x+w-8 && my>=yy && my<yy+28;
            g.fill(x+6,yy,x+w-6,yy+27,row.selected()?0xFF4C4028:hover?0xFF38313F:0xFF25212B);
            var icon=icon(row);int tx=x+13;
            if(icon.isPresent()) {g.blit(RenderPipelines.GUI_TEXTURED,icon.get(),tx,yy+5,0f,0f,18,18,32,32,32,32);tx+=24;}
            String name=font.plainSubstrByWidth((scroll+n+1)+". "+row.name(),Math.max(20,w-(tx-x)-120));
            int color=row.study()<100?0xFFFF5555:row.sequence()<ClientPathwayData.getSequence()?0xFF827889:0xFFF4EBD0;
            g.text(font,name,tx,yy+5,color);
            g.text(font,"Seq "+row.sequence(),tx,yy+16,0xFFB4A7BD);
            g.text(font,row.study()<100?String.format(Locale.ROOT,"%.1f%%",Math.floor(row.study()*10)/10):"100%",x+w-92,yy+9,row.study()<100?0xFFFF5555:0xFFD1A94B);
            if(hover)g.setTooltipForNextFrame(font,font.split(Component.literal(row.name()+(row.study()<100?" — Requires 100% study":"")),Math.min(300,width-24)),mx,my);
            g.text(font,row.favorite()?"★":"☆",x+w-27,yy+9,0xFFFFD577);
        }
        if(rows.isEmpty())g.centeredText(font,"Watch abilities being cast to study them.",width/2,y+50,0xFFF4EBD0);
        g.text(font,"Left click: select   Star: favorite   Scroll: browse",x+10,bottom+8,0xFFB4A7BD);
        if(rows.size()>visible()) {
            int track=visible()*28,thumb=Math.max(8,track*visible()/rows.size());
            int sy=y+30+(track-thumb)*scroll/Math.max(1,rows.size()-visible());
            g.fill(x+w-4,sy,x+w-1,sy+thumb,0xFFD1A94B);
        }
    }
}
