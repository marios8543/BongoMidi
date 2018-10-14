package com.bongo;

class Note {

    final Integer midi_number;
    final Integer channel;
    private final Integer patch;
    final Renderer.Instr_Categ cpatch;
    final Boolean status;

    Note(Integer channel, Integer midi_number, Integer instrument, Boolean status){
        this.patch = instrument;
        this.channel = channel;
        if(channel==9) {
            this.cpatch = Renderer.Instr_Categ.Percussion;
        }
        else{
            this.cpatch = Renderer.Instr_Categ.values()[this.patch/8];
        }
        this.midi_number = midi_number;
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Note: %d - Channel: %d - Patch: %d (Category: %s) - Status: %s",this.midi_number,this.channel,this.patch,this.cpatch,this.status);
    }
}
