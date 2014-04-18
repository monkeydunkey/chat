/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.alljoyn.bus.sample.chat;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;


@BusInterface (name = "org.alljoyn.bus.samples.chat")
public interface ChatInterface {
    /*
     * The BusSignal annotation signifies that this function should be used as
     * part of the AllJoyn interface.  The runtime is smart enough to figure
     * out that this is a used as a signal emitter and is only called to send
     * signals and not to receive signals.
     */
    @BusSignal
    public void Notify(String str, String nickname, double key) throws BusException;
    @BusSignal
    public void nickname(String usrname , String all_unique, Boolean mob_or_desk)throws BusException;
    @BusSignal
    public void validate(boolean val)throws BusException;
    @BusSignal
    public void sendKey(Double a)throws BusException;
    @BusSignal
    public void askKey(String name)throws BusException;
    @BusSignal
    public void send_message(String message,String nick);
    
    @BusSignal
    public void music_data(byte[] data) throws BusException;
    
    @BusSignal
    public void clock_sync(long countdown )throws BusException;
    
    @BusSignal
    public void delay_est(long time_stamp,long time_stamp_pre) throws BusException;
    
    @BusSignal
    public void song_change(long duration) throws BusException;
    
    @BusSignal
    public void re_sync() throws BusException;
    
}

