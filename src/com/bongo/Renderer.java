package com.bongo;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

public abstract class Renderer {
    private static ArrayList<Integer> xcoords = new ArrayList<Integer>();
    private static ArrayList<Integer> ycoords = new ArrayList<Integer>();
    private static ArrayList<Integer[]> coords = new ArrayList<Integer[]>();
    private static Random rand = new Random();

    public static void build_coords(){
        for(Integer i=0;i<1024;i++){
            xcoords.add(i);
            i+=300;
        }
        for(Integer i=0;i<720;i++){
            ycoords.add(i);
            i+=184;
        }
        for(int a : xcoords){
            for(int b :ycoords){
                coords.add(new Integer[]{a,b});
            }
        }
    }

    public static class Bongo {
        public Integer x;
        public Integer y;
        public Note note;
        public Boolean l_hand=false;
        public Boolean r_hand=false;

        public Bongo(Note note) {
            Random rand = new Random();
            if(Main.bongos[note.channel]!=null){
                this.x = Main.bongos[note.channel].x;
                this.y = Main.bongos[note.channel].y;
            }
            else {
                Integer[] c = coords.get(15-note.channel);
                this.x = c[0];
                this.y = c[1];
            }
            this.note = note;
            if(note.status){
                if(note.cpatch==Instr_Categ.Percussion || note.cpatch==Instr_Categ.CPerc){
                    if(note.midi_number<=36){
                        this.l_hand=true;
                    }
                    else {
                        this.r_hand=true;
                    }
                }
                else {
                    if(note.midi_number<60){
                        this.l_hand=true;
                    }
                    else{
                        this.r_hand=true;
                    }
                }
            }
        }
        @Override
        public String toString(){
            return String.format("X:%d - Y:%d - L:%b R:%b - %s",this.x,this.y,this.l_hand,this.r_hand,this.note.toString());
        }
    }

    private enum coords{

    }

    static ClassLoader classLoader = Renderer.class.getClassLoader();
    public static Image load_asset(String asset){
        try{
            return ImageIO.read(classLoader.getResource(asset)).getScaledInstance(380,264,Image.SCALE_FAST);
        }
        catch (Exception e){
            return null;
        }
    }
    public enum Instr_Categ{
        Piano(load_asset("keyboard.png")),
        CPerc(load_asset("drums.png")),
        Organ(load_asset("organ.png")),
        Guitar(load_asset("guitar.png")),
        Bass(load_asset("guitar.png")),
        Strings(load_asset("strings.png")),
        Ensemble(load_asset("ensemble.png")),
        Brass(load_asset("brass.png")),
        Reed(load_asset("reed.png")),
        Pipe(load_asset("pipe.png")),
        Synth_lead(load_asset("keyboard.png")),
        Synth_pad(load_asset("keyboard.png")),
        Synth_fx(load_asset("keyboard.png")),
        Ethnic(load_asset("ensemble.png")),
        Percussion(load_asset("drums.png")),
        FX(load_asset("ensemble.png"));
        private Image asset;
        Instr_Categ(Image s){
            this.asset = s;
        }
        public Image getAsset(){
            return asset;
        }
    }

    public enum hands{
        lh1(load_asset("l1.png")),
        lh2(load_asset("l2.png")),
        rh1(load_asset("r1.png")),
        rh2(load_asset("r2.png"));
        private Image hand;
        hands(Image s){
            this.hand = s;
        }
        public Image get_hand(){
            return hand;
        }
    }
    public static Image get_lhand(Boolean state) {
        if (!state) {
            return hands.lh1.get_hand();
        }
        return hands.lh2.get_hand();
    }

    public static Image get_rhand(Boolean state) {
        if(state == false){
            return hands.rh1.get_hand();
        }
        return hands.rh2.get_hand();
    }
}
