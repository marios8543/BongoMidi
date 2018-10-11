package com.bongo;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;

abstract class Renderer {
    private static ArrayList<Integer> xcoords = new ArrayList<>();
    private static ArrayList<Integer> ycoords = new ArrayList<>();
    private static ArrayList<Integer[]> coords = new ArrayList<>();

    static void build_coords(){
        for(Integer i=0;i<1024;i+=301){
            xcoords.add(i);
        }
        for(Integer i=0;i<720;i+=185){
            ycoords.add(i);
        }
        for(int a : xcoords){
            for(int b :ycoords){
                coords.add(new Integer[]{a,b});
            }
        }
    }

    public static class Bongo {
        Integer x;
        Integer y;
        Note note;
        Boolean l_hand=false;
        Boolean r_hand=false;

        Bongo(Note note) {
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
            if(Main.bongos[note.channel]!=null){
                this.x = Main.bongos[note.channel].x;
                this.y = Main.bongos[note.channel].y;
                this.l_hand = !Main.bongos[note.channel].l_hand;
                this.r_hand = !Main.bongos[note.channel].r_hand;
            }
            else {
                Integer[] c = coords.get(15-note.channel);
                this.x = c[0];
                this.y = c[1];
            }
            this.note = note;

        }
        @Override
        public String toString(){
            return String.format("X:%d - Y:%d - L:%b R:%b - %s",this.x,this.y,this.l_hand,this.r_hand,this.note.toString());
        }
    }

    private static ClassLoader classLoader = Renderer.class.getClassLoader();

    private static Image load_asset(String asset){
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
    static Image get_lhand(Boolean state) {
        if (!state) {
            return hands.lh1.get_hand();
        }
        return hands.lh2.get_hand();
    }

    static Image get_rhand(Boolean state) {
        if(!state) return hands.rh1.get_hand();
        return hands.rh2.get_hand();
    }
}
