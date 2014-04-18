/*
 * Copyright (c) 2010-2011, 2013, AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

/*
 Author Shashank
 */
package org.alljoyn.bus.sample.chat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.tritonus.share.sampled.file.TAudioFileFormat;

public class Service {

    static {
        System.loadLibrary("alljoyn_java");
    }

    // Start of variable Declarations
    //Variables related to alljoyn session establishemnt
    static boolean mSessionEstablished = false;
    static int mSessionId;
    static String mJoinerName;
    private static final String NAME_PREFIX = "org.alljoyn.bus.samples.chat";
    private static final short CONTACT_PORT = 27;
    private static BusAttachment mBus;
    static int mUseSessionId = -1;
    private static double key = (Math.random() * 100000) + 1;           //Generating random secret key for the device

    private static ArrayList<String> Alljoyn_unique_name;               //stores the nicknames provided to devices by alljoyn
    static ArrayList<String> nickname;                                  //stores the nicknames chosen by the user
    private static ArrayList<String> MissedCalls;                       //stores the calls missed by the user
    private static ArrayList<String> Alljoyn_unique_name_desk;
    private static ArrayList<String> nickname_desk;
    private static ArrayList<String> Alljoyn_unique_name_mob;
    private static ArrayList<String> nickname_mob;
    
    
    private static double[] keys = new double[100];                     //stores the all the keys it has recieved
    private static int key_count = 0;
    private static ArrayList<String> channels;                          //Arraylist for storing all the visible Alljoyn Channel
    private static ArrayList<String> notification_received;             //Arraylist for storing all the current call notifications
    private static ArrayList<String> notification_received_mem;
    private static ArrayList<messageTh> notification_thread;            //Arraylist for storing the threads of the message box GUI

    private static Boolean running;                                     //This is used to stop the program. Setting it to false will stop the serve program
    private static int ask_key_ind = -1;                                //This stores the device index in the device list from whoes key user wishes to get

    private static ChatInterface myInterface = null;                    //This is the interface through which all of the alljoyn signals are sent
    private static SignalInterface mySignalInterface;                   //This the interface through which the alljoyn signal emitter is intiated
    static String channel_name = null;                                  //Stores the name of channel that the user has chosen
    private static Random rand;                                         //randdom number genrator used to select error message
    //This list contains error messges from which any one is picked and shown randomly
    private static String[] ErrorList = {"A monkey Threw a wrench in the gears. Please try again",
        "Monkeys are attacking us again. Try again please",
        "Pigs are flying.That seems to be reason for the crash",
        "Looks like our app went for a vacation. Dont worry we shall bring it back.",
        "The flying monkeys are here, we better hide. Dont worry it's only till our reinforcements arrive."};

    private static FileInputStream in;                  //this stores the FileInputStream object of the current song
    private static long previous_time;                  //This used for syncing purpose
    private static Timer t1;                            //This timer is used to schedule the player
    private static TimerTask music_player;              //This is the task that handles the music player
    private static long delay = 0;                      //this stores the Round trip time of a communication with other devices
    private static int delay_count = 0;
    private static long curr_file_dur;                  //This stores the current music file duration/track length
    private static Thread music_player_handler = null;  //this handles the continuous music playing
    private static Boolean music_player_running = true; //this is used to specify whether the music is to be played or not
    private static data_transfer_thread data_transfer_handler = null;   //this handles the data transfer from service to various clients 
    private static Boolean data_transfer = true;                        //this specifies whether data_transfer should occur or not
    private static ArrayList<String> filelist;                          //this stores all the names of the music files to be played
    private static musicPlayerThread mp3player = null;                  //this stores the object of the music player
    private static long[] music_duration;                               //this stores the duration of all music files

    //End of Variable Declarations
    // This is used to send message/notification response to the calling device
    public static void sendMessage(String s, String uni) throws BusException {
        SignalEmitter emitter = new SignalEmitter(mySignalInterface, uni, mSessionId, SignalEmitter.GlobalBroadcast.Off);
        ChatInterface usrInterface = emitter.getInterface(ChatInterface.class);
        usrInterface.Notify(s, nickname.get(0), 0);
        
        int ind = notification_received.indexOf(uni);
        notification_received.remove(ind);
        notification_received_mem.remove(ind);
        notification_thread.remove(ind);
    }

    // This method is used to add missed calls to the missed call arraylist
    public static void call_missed(String s, String alljoyn_uni){
        int ind = notification_received.indexOf(alljoyn_uni);
        MissedCalls.add(notification_received_mem.get(ind) + " - " + s);
        
        notification_received.remove(ind);
        notification_received_mem.remove(ind);
        notification_thread.remove(ind);
    }

    //This static method is called when the user wants to send a message vis group chat
    public static void send_msg(String message) {
        if (myInterface != null) {
            myInterface.send_message(message, nickname.get(0));
        }

    }

    // The signal interface is used to send data using alljoyn's signals
    public static class SignalInterface implements ChatInterface, BusObject {

        //Signal via which all the notifications are to be sent
        public void Notify(String s, String nickname, double key) throws BusException {
        }

        //Signal via which all the nickname of new users are to be sent
        @Override
        public void nickname(String usrname, String Alljoyn_unique_nameque,Boolean mob_or_desk) throws BusException {
        }

        //Signal via which the Service/Channel creator validates a new users nickname
        @Override
        public void validate(boolean val) throws BusException {
        }

        //Signal via which users can send their private keys to other devices, thus enabling them to receive their notifications
        @Override
        public void sendKey(Double a) throws BusException {
        }

        //Signal via which a user can ask other for their unique key
        @Override
        public void askKey(String s) throws BusException {
        }

        //Signal via which the user can send messages to all other users. It is used for group chat 
        @Override
        public void send_message(String message, String nick) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void music_data(byte[] data) throws BusException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void clock_sync(long countdown) throws BusException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void delay_est(long time_stamp, long time_stamp_pre) throws BusException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void song_change(long duration) throws BusException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void re_sync() throws BusException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    //The signal handler reads the signals sent to the device by other devices
    public static class SignalHandler {

        //It handles the notification the PC has received
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "Notify")
        public void Notify(String string, String nick, double key1) {
            System.out.println("here here");
            Boolean key_exist = false;
            for (int i = 0; i < 100; i++) {
                if (keys[i] == key1) {
                    key_exist = true;
                    break;
                } else if (keys[i] == -1) {
                    break;
                }
            }
            if (key_exist || key1 == 0 || key == key1) {
                if (!string.equals("call cancelled or received")) {
                    System.out.println("fuck");
                    if(!notification_received_mem.contains(nick)){
                        System.out.println("fuck1");
                        final String f = string;
                        MessageContext ctx = mBus.getMessageContext();
                        String nickname = ctx.sender;
                        String as = string;
                        messageTh sas = new messageTh(as, nickname);
                        sas.start();
                        //Incoming notifications' user name and the corresponding gui thread are stored
                        notification_received.add(nickname);
                        notification_received_mem.add(nick);
                        notification_thread.add(sas);
                        //For Debugging purpose
                        nickname = nickname.substring(nickname.length() - 10, nickname.length());
                        System.out.println(nickname + ": " + string);
                    }
                } else {
                    int ind = notification_received_mem.indexOf(nick);
                    messageTh temp = notification_thread.get(ind);
                    //The notification thread is stopped
                    temp.stop_thread();

                    // If the user rejects or receives the call from the mobile the notification's properties are removed from the list
                    notification_received.remove(ind);
                    notification_received_mem.remove(ind);
                    notification_thread.remove(ind);
                }
            }

        }

        //This is signal handler responds to the nickname allocation request by clients. True if the nickname is unique and user is allocated that name and false otherwise  
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "nickname")
        public void nickname(String usrname, String Alljoyn_unique_nameque,Boolean desk_or_mob) throws BusException {
            System.out.println("!!!Validation Called!!!");
            SignalEmitter emitter = new SignalEmitter(mySignalInterface, Alljoyn_unique_nameque, mSessionId, SignalEmitter.GlobalBroadcast.Off);
            ChatInterface usrInterface = emitter.getInterface(ChatInterface.class);

            if (!nickname.contains(usrname)) {
                nickname.add(usrname);
                Alljoyn_unique_name.add(Alljoyn_unique_nameque);
                usrInterface.validate(true);
                if(desk_or_mob){
                    nickname_desk.add(usrname);
                    Alljoyn_unique_name_desk.add(Alljoyn_unique_nameque);
                }
                else{
                    nickname_mob.add(usrname);
                    Alljoyn_unique_name_mob.add(Alljoyn_unique_nameque);
                }

            } else {
                usrInterface.validate(false);
            }

        }

        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "validate")
        public void validate(boolean val) {
        }

        //This signal handler is used to receive the keys sent to the device
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "sendKey")
        public void sendKey(Double a) {
            if (a == -1.0) {
                JOptionPane.showMessageDialog(null, "The device did not shared it's key");
            } else {
                keys[key_count] = a;
                key_count++;
            }
        }

        //This method handler is used to respond to request from other devices
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "askKey")
        public void askKey(String name) throws BusException {

            SignalEmitter emitter = new SignalEmitter(mySignalInterface, name, mSessionId, SignalEmitter.GlobalBroadcast.Off);
            ChatInterface usrInterface = emitter.getInterface(ChatInterface.class);
            System.out.println("it's here");
            if (JOptionPane.showConfirmDialog(App.gui,
                    "Are you sure you want to send your key?", "Are you sure?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {

                usrInterface.sendKey(key);
            } else {
                usrInterface.sendKey(-1.0);
            }
        }

        //This signal handler, handles the incoming group chat messages
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "send_message")
        public void send_message(String message, String nick) {
            String dis = nick + " : " + message;
            Chat_messageGUI.add_message(dis);
        }

        //This is used for clock syncing purpose
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "clock_sync")
        public void clock_sync(long count_down) throws BusException, FileNotFoundException {

        }

        //This is used for estimating the delay of the network
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "delay_est")
        public void delay_est(long time_stamp, long time_stamp_pre) throws IOException, BusException, InterruptedException {
            delay_count++;
            System.out.println("it hererer");
            long received = System.currentTimeMillis();
            if (received - previous_time > delay) {
                delay = received - previous_time;
            }
            if (delay_count >= 2) {
                System.out.println("delay" + delay);
                myInterface.clock_sync(2 * delay);
                delay_count = 0;
                asyncMusicPlay(2 * delay);

            } else {
                Thread.sleep(60);
                System.out.println("pre gets updated");
                myInterface.delay_est(System.currentTimeMillis(), received);
                previous_time = System.currentTimeMillis();
            }
        }

    }

    //The MethodHandler provides implemention for the GroupInterface which contains declarations for alljoyn methods
    public static class MethodHandler implements GroupInterface, BusObject {

        public void preDispatch() {
        }

        public void postDispatch() {
        }

        //Method via which a device can ask from other device for its key
        //Method via which a device can ask from the service/channel creator for the user assigned nicknames of the devices connected 
        @Override
        public synchronized String[] getMem() throws BusException {
            String[] temp = new String[nickname.size()];
            for (int i = 0; i < nickname.size(); i++) {
                temp[i] = nickname.get(i);
            }
            return temp;
        }

        //Method via which a device can ask from the service/channel creator for the Alljoyn nicknames of the devices connected
        @Override
        public synchronized String[] getUni() throws BusException {
            String[] temp = new String[Alljoyn_unique_name.size()];
            for (int i = 0; i < Alljoyn_unique_name.size(); i++) {
                temp[i] = Alljoyn_unique_name.get(i);
            }
            return temp;
        }

        @Override
        public String[] get_mob_uni() throws BusException {
            String[] temp = new String[Alljoyn_unique_name_mob.size()];
            for (int i = 0; i < Alljoyn_unique_name_mob.size(); i++) {
                temp[i] = Alljoyn_unique_name_mob.get(i);
            }
            return temp;
        }

        @Override
        public String[] get_mob_mem() throws BusException {
            String[] temp = new String[nickname_mob.size()];
            for (int i = 0; i < nickname_mob.size(); i++) {
                temp[i] = nickname_mob.get(i);
            }
            return temp;
        }

        @Override
        public String[] get_des_uni() throws BusException {
            String[] temp = new String[Alljoyn_unique_name_desk.size()];
            for (int i = 0; i < Alljoyn_unique_name_desk.size(); i++) {
                temp[i] = Alljoyn_unique_name_desk.get(i);
            }
            return temp;
        }

        @Override
        public String[] get_des_mem() throws BusException {
            String[] temp = new String[nickname_desk.size()];
            for (int i = 0; i < nickname_desk.size(); i++) {
                temp[i] = nickname_desk.get(i);
            }
            return temp;
        }

    }

    //This creates a new thread on which music player handler is to be run 
    public static void asyncMusicPlay(final long delay) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    play_music(delay);
                } catch (Exception ex) {
                    System.out.println("music player handler cannot be started");
                }
            }
        };
        music_player_handler = new Thread(task, "musicThread");
        music_player_handler.start();
    }

    //this method handles the continuous playing of music 
    public static void play_music(long delay) throws InterruptedException, IOException {

        mp3player = new musicPlayerThread(in);
        Timer t1 = new Timer(true);
        t1.schedule(mp3player, delay);
        Thread.sleep(delay + curr_file_dur);
        in.close();
        for (int j = 1; j < filelist.size(); j++) {
            in = new FileInputStream(filelist.get(j));
            mp3player = new musicPlayerThread(in);
            t1 = new Timer(true);
            System.out.println("player is about to start");
            t1.schedule(mp3player, 500);

            Thread.sleep(curr_file_dur);

            in.close();
            if (!music_player_running) {
                break;
            }
        }
    }

    //This method is called after the user has selected the music he/she wants to play and is ready to start playing the songs
    public static void play() throws FileNotFoundException, UnsupportedAudioFileException, IOException, BusException, InterruptedException {
        System.out.println("current time" + System.currentTimeMillis());
        music_duration = new long[filelist.size()];
        for (int j = 0; j < filelist.size(); j++) {
            music_duration[j] = DurationWithMp3Spi(filelist.get(j));
        }

        sync();

    }

    //This method is used to stop the music player and the data transfer
    public static void stop() throws BusException, IOException {
        if (mp3player != null) {
            mp3player.stop();
        }
        if (data_transfer_handler != null) {
            data_transfer_handler.set_running(false);
        }
        music_player_running = false;
        myInterface.re_sync();
        delay_count = 0;
        if (in != null) {
            in.close();
        }

    }

    //This method is used to initialize the filelist which stores the names of all the files
    //that are to be played
    public static void add_song(ArrayList<String> arr) {
        filelist = new ArrayList<String>();
        for (int i = 0; i < arr.size(); i++) {
            filelist.add(arr.get(i));
        }
    }

    //This initiates the sync process. It initializes all variables 
    public static void sync() throws UnsupportedAudioFileException, IOException, BusException, InterruptedException {
        if (mp3player != null) {
            mp3player.stop();
        }
        if (data_transfer_handler != null) {
            data_transfer_handler.set_running(false);
        }
        music_player_running = false;
        myInterface.re_sync();
        delay_count = 0;
        if (in != null) {
            in.close();
        }
        Thread.sleep(100);
        in = new FileInputStream(filelist.get(0));
        previous_time = System.currentTimeMillis();

        // a new data_transfer_handler is initialized
        data_transfer_handler = new data_transfer_thread(filelist, myInterface, music_duration);
        data_transfer_handler.start();

    }

    //It sets the duration of the current music file
    public static void set_curr_file_dur(long dur) {
        System.out.println("curr file duration" + dur);
        curr_file_dur = dur;
    }

    //MyBuslistener is a child class of Alljoyn Buslistener class which listens for activity on the channel and calls appropriatecall back methods 
    private static class MyBusListener extends BusListener {

        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            System.out.println(String.format("BusListener.foundAdvertisedName(%s, %d, %s)", name, transport, namePrefix));
            channels.add(name.substring(29));

        }

        public void nameOwnerChanged(String busName, String previousOwner, String newOwner) {
            if ("com.my.well.known.name".equals(busName)) {
                System.out.println("BusAttachement.nameOwnerChanged(" + busName + ", " + previousOwner + ", " + newOwner);
            }
        }

        public void lostAdvertisedName(String name, short transport, String namePrefix) {
            String channel_name = name.substring(29);
            if (channels.contains(channel_name)) {
                System.out.println("LostAdvertisedName " + name);
                channels.remove(channel_name);
                System.out.println(channels.size());
            }
        }

    }

    //Static method which is called by the GUI when the user sets a channel name
    public static void Set_Channel_Name(String text) {
        channel_name = text;
    }

    //This method is used to set the users nickname
    public static void Set_nickname(String name) {
        nickname.add(name);
        nickname_desk.add(name);
    }

    //This method is used to set running to the appropriate value, setting it false will stop the thread
    public static void set_running(Boolean run) {
        running = run;
    }

    //This method returns the currently connected devices nicknames in a array of strings
    public static String[] get_device_list() {
        String[] temp = new String[nickname.size()];
        for (int i = 0; i < nickname.size(); i++) {
            temp[i] = nickname.get(i);
        }
        return temp;
    }

    //This method is used to set the device index from which key is to be requested
    public static void set_ask_key_ind(int ind) {
        ask_key_ind = ind;
    }

    //This method ask other devices for their authorization key
    public static void ask_key() throws InterruptedException, BusException {
        ask_key_ind = -1;
        String[] uni_names = new String[Alljoyn_unique_name_mob.size()];
        final String[] nick = new String[nickname_mob.size()];
        for (int i = 0; i < nickname_mob.size(); i++) {
            uni_names[i] = Alljoyn_unique_name_mob.get(i);
            nick[i] = nickname_mob.get(i);
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AskKey(nick, 1, nickname.get(0)).setVisible(true);
            }
        });
        while (ask_key_ind == -1 && running) {
            if (ask_key_ind == -2) {
                return;
            }
            Thread.sleep(100);
        }
        if (ask_key_ind == -2) {
            return;
        }
        if (running) {
            SignalEmitter emitter = new SignalEmitter(mySignalInterface, uni_names[ask_key_ind], mUseSessionId, SignalEmitter.GlobalBroadcast.Off);
            ChatInterface usrInterface = emitter.getInterface(ChatInterface.class);
            usrInterface.askKey(Alljoyn_unique_name.get(0));
        }
    }

    //This method starts the GUI which shows user all the call they have missed
    public static void see_missed_calls() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Call_Notification(MissedCalls).setVisible(true);
            }
        });
    }

    /**
     * Static method which runs the Service or Channel Creator The initial flow
     * is same a client, so please refer to the long comment in the client code.
     * The differences are The server's creator needs no validation of his/her
     * chosen nickname. The device's port is required to be binded now(explained
     * above bindSessionPort) The channel name selected is first requested if
     * request is successful i.e. channel name does not exist, the name is
     * granted. Now the server waits for a device to be connected.
     */
    public static void run_service(Boolean run) throws BusException, FileNotFoundException, UnsupportedEncodingException {
        //Static variables are initialised in order to flush out any residual value from some earlier run of the thread
        running = run;
        key_count = 0;
        MissedCalls = new ArrayList<String>();

        mySignalInterface = null;
        myInterface = null;
        channels = new ArrayList<String>();
        channels.add("nan");
        
        Alljoyn_unique_name = new ArrayList<String>();
        Alljoyn_unique_name_mob=new ArrayList<String>();
        Alljoyn_unique_name_desk=new ArrayList<String>();
        
        nickname = new ArrayList<String>();
        nickname_mob = new ArrayList<String>();
        nickname_desk = new ArrayList<String>();
        
        rand = new Random();
        //Initializing all the nicknames
        for (int i = 0; i < 100; i++) {
            keys[i] = -1;
        }
        channel_name = null;
        notification_thread=new ArrayList<messageTh>();
        notification_received=new ArrayList<String>();
        notification_received_mem=new ArrayList<String>();
      
        //End of variable initialization

        //mBus is the object which connects to the Alljoyn bus daemon
        mBus = new BusAttachment("org.alljoyn.bus.samples", BusAttachment.RemoteMessage.Receive);

        Status status;

        mySignalInterface = new SignalInterface();

        //The signal interface via which signals are sent are registerd with the alljoyn bus attachment
        status = mBus.registerBusObject(mySignalInterface, "/chatService");
        if (status != Status.OK) {
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            return;
        }
        System.out.println("BusAttachment.registerBusObject successful");

        //A bus listener that listens to activity on the alljoyn bus is registered with the bus attachemnt
        BusListener listener = new MyBusListener();
        mBus.registerBusListener(listener);

        //The bus attachment is connected to the Alljoyn virtual bus
        status = mBus.connect();
        if (status != Status.OK) {
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            return;
        }
        System.out.println("BusAttachment.connect successful on " + System.getProperty("org.alljoyn.bus.address"));

        SignalHandler mySignalHandlers = new SignalHandler();

        //The signal handlers are registered with the bus attachment
        status = mBus.registerSignalHandlers(mySignalHandlers);
        if (status != Status.OK) {
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            return;
        }
        System.out.println("Signal Handler registered");

        MethodHandler mySampleService = new MethodHandler();

        //The method handler are regisered with the bus attachment
        status = mBus.registerBusObject(mySampleService, "/chatService");
        if (status != Status.OK) {
            System.out.println(status);
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            return;
        }

        System.out.println("Method handler Registered");

        //Channel discovery is initiated so get the names of all the available channel which have the same initial well known name
        mBus.findAdvertisedName(NAME_PREFIX);

        //The alljoyn unique anem for the devices is stored
        Alljoyn_unique_name.add(mBus.getUniqueName());
        Alljoyn_unique_name_desk.add(mBus.getUniqueName());

        Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);

        SessionOpts sessionOpts = new SessionOpts();
        sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
        sessionOpts.isMultipoint = true;
        sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
        sessionOpts.transports = SessionOpts.TRANSPORT_ANY;

        // This binds the port of the device with alljoyn bus whenver a device tries 
        //to join the channel appropriate methods are called
        status = mBus.bindSessionPort(contactPort, sessionOpts,
                new SessionPortListener() {
                    public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                        System.out.println("SessionPortListener.acceptSessionJoiner called");
                        if (sessionPort == CONTACT_PORT) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    public void sessionJoined(short sessionPort, int id, String joiner) {
                        System.out.println(String.format("SessionPortListener.sessionJoined(%d, %d, %s)", sessionPort, id, joiner));
                        mSessionId = id;
                        mJoinerName = joiner;
                        System.out.println(mJoinerName);
                        mSessionEstablished = true;
                        mUseSessionId = id;

                        SignalEmitter emitter = new SignalEmitter(mySignalInterface, mSessionId, SignalEmitter.GlobalBroadcast.Off);
                        myInterface = emitter.getInterface(ChatInterface.class);
                        mBus.setSessionListener(id, new SessionListener() {
                            public void sessionMemberRemoved(int sessionId, String uniqueName) {
                                System.out.println("member left");
                                int i = Alljoyn_unique_name.indexOf(uniqueName);
                                Alljoyn_unique_name.remove(i);
                                nickname.remove(i);
                            }
                        }
                        );
                    }
                });
        if (status != Status.OK) {
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            return;
        }
        System.out.println("BusAttachment.bindSessionPort successful");

        //This starts the channel name and nickname GUI required for channel creation
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Create_Channel(channels).setVisible(true);
            }
        });

        //The thread waits for the user to enter a channel name and a nickname
        while (channel_name == null && running) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            }
        }

        System.out.println(channel_name);

        //The channel name selected by the user is requested for allocation by the alljoyn bus
        String wellKnownName = NAME_PREFIX + "." + channel_name;
        int flags = 0; //do not use any request name flags
        status = mBus.requestName(wellKnownName, flags);
        if (status != Status.OK) {
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            return;
        }
        System.out.println("BusAttachment.request 'com.my.well.known.name' successful");

        //The channel name selected by the user is advertised to all other alljoyn devices
        status = mBus.advertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
        if (status != Status.OK) {
            System.out.println("Status = " + status);
            mBus.releaseName(wellKnownName);
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            return;
        }
        System.out.println("BusAttachment.advertiseName 'com.my.well.known.name' successful");

        if (running) {
            App.set_channel_nickname(channel_name, nickname.get(0));
        }

        try {
            while (!mSessionEstablished && running) {
                Thread.sleep(10);

            }
            System.out.println("Server running");

            //This is to run the service infinetly
            while (true && running) {

                Thread.sleep(10000);
                myInterface.Notify("service_message", nickname.get(0), 0);
            }
        } catch (InterruptedException ex) {
            System.out.println("Interrupted");
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
        }
        System.out.println("Service exiting");
        mBus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
        mBus.disconnect();
        App.on_close();
    }

    public static void main(String[] args) throws BusException {

    }

    //This method is used to calculate the duration of the a music file whose path and name is specified by string s 
    private static long DurationWithMp3Spi(String s) throws UnsupportedAudioFileException, IOException {
        System.out.println("file name " + s);
        File f1 = new File(s);
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(f1);
        if (fileFormat instanceof TAudioFileFormat) {
            Map<?, ?> properties = ((TAudioFileFormat) fileFormat).properties();
            String key = "duration";
            Long microseconds = (Long) properties.get(key);
            long mili = (microseconds / 1000);
            long sec = (mili / 1000) % 60;
            long min = (mili / 1000) / 60;
            System.out.println("time = " + min + ":" + sec);
            return mili;
        } else {
            throw new UnsupportedAudioFileException();
        }

    }

}

//This class creates a new thread on which a new jFrame is created for displaying the incoming notification
class messageTh extends Thread {

    final String f;
    final String all_uni;
    PopUpNotification ch;

    public messageTh(String s, String uni) {
        f = s;
        all_uni = uni;
    }

    public void stop_thread() {
        ch.dispose();
    }

    public void run() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ch = new PopUpNotification(f, all_uni, 1);
                ch.setVisible(true);
            }
        });
    }
}

//This thread implementation is used to run the music player
class musicPlayerThread extends TimerTask {

    FileInputStream data;
    static Player mp3player;

    public musicPlayerThread(FileInputStream in) {
        this.data = in;
    }

    public void stop() {
        mp3player.close();
    }

    public void run() {

        try {
            System.out.println("player " + System.currentTimeMillis());
            mp3player = new Player(data);
            mp3player.play();
        } catch (JavaLayerException ex) {
            Logger.getLogger(musicPlayerThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

//This thread implementation is used for handling the data transfer to the clients
class data_transfer_thread extends Thread {

    public ArrayList<String> filelist;
    public ChatInterface myInterface;
    public long[] music_duration;
    public Boolean running;

    public data_transfer_thread(ArrayList<String> files, ChatInterface Interface, long[] music_length) {
        filelist = files;
        myInterface = Interface;
        music_duration = music_length;
        running = true;
    }

    public void set_running(Boolean flag) {
        running = false;
    }

    public void run() {
        System.out.println("Data transer starts");
        FileInputStream in2;
        try {
            in2 = new FileInputStream(filelist.get(0));
            myInterface.delay_est(System.currentTimeMillis(), 0);
            for (int j = 0; j < filelist.size(); j++) {
                if (running) {
                    in2 = new FileInputStream(filelist.get(j));
                    long curr_file_dur = music_duration[j];
                    Service.set_curr_file_dur(curr_file_dur);
                    myInterface.song_change(music_duration[j]);
                    System.out.println("song change " + curr_file_dur);

                    int sleep_count = 0;
                    while (in2.available() > 0 && running) {
                        //System.out.println("data transfered");
                        int len = in2.available();
                        if (len > 50000) {
                            len = 50000;
                        }
                        byte[] data = new byte[len];
                        in2.read(data, 0, len);
                        if (running) {
                            myInterface.music_data(data);
                        }
                        sleep_count++;
                        Thread.sleep(50);

                    }
                    in2.close();
                    Thread.sleep(curr_file_dur - (sleep_count * 150));
                } else {
                    break;
                }
                System.out.println("data transfer stops");
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(data_transfer_thread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BusException ex) {
            Logger.getLogger(data_transfer_thread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(data_transfer_thread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(data_transfer_thread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
