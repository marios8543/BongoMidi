package com.bongo;
import javax.sound.midi.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class MidiParser {
    Sequencer sequencer;

    MidiParser(File file) throws Exception{
        sequencer = MidiSystem.getSequencer();
        Sequence seq = MidiSystem.getSequence(file);
        sequencer.setSequence(seq);
        sequencer.open();
        Transmitter transmitter = sequencer.getTransmitter();
        transmitter.setReceiver(new creceiver());
    }


    static class creceiver implements Receiver {
        final Map<Integer,Integer> v_instrs = new HashMap<>();
        final ArrayList<Integer> v_ons = new ArrayList<>();
        final ArrayList<Integer> v_offs = new ArrayList<>();

        creceiver(){
            for(int i=192;i<=207;i++){
                v_instrs.put(i,0);
            }
            for(int i=0;i<16;i++){
                v_ons.add(i+144);
                v_offs.add(i+128);
            }
        }

        Note note;

        @Override
        public void send(MidiMessage message, long timeStamp){
            note = process_message(message);
            if(note!=null){
                Main.bongos[note.channel] = new Renderer.Bongo(note);
            }
        }

        public void close(){}

        Note process_message(MidiMessage message){
            if(v_instrs.containsKey(message.getStatus())){
                Byte patch = message.getMessage()[1];
                v_instrs.put(message.getStatus(),patch.intValue());
                return null;
            }
            if(v_ons.contains(message.getStatus())){
                Byte patch = message.getMessage()[1];
                if(message.getStatus()==153){
                    return new Note(9,patch.intValue(),patch.intValue(),true);
                }
                else {
                    return new Note(v_ons.indexOf(message.getStatus()), patch.intValue(), v_instrs.get(v_ons.indexOf(message.getStatus()) + 192), true);
                }
            }
            else if(v_offs.contains(message.getStatus())){
                Byte patch = message.getMessage()[1];
                if(message.getStatus()==153){
                    return new Note(9,patch.intValue(),patch.intValue(),false);
                }
                else {
                    return new Note(v_offs.indexOf(message.getStatus()),patch.intValue(),v_instrs.get(v_offs.indexOf(message.getStatus())+192),false);
                }
            }
            return null;
        }
    }
}
