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


package org.alljoyn.bus.sample.chat;

/**
 *
 * @author Shashank
 */
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.SignalEmitter;

import java.util.Scanner;
import javax.swing.JOptionPane;
import org.alljoyn.bus.ProxyBusObject;

public class Client implements Runnable {

    static {
        System.loadLibrary("alljoyn_java");
    }

    
    ///Start of variable declarations
    //////Variables realted to alljoyn connection establishment
    private static final short CONTACT_PORT = 27;                           // port no on which alljoyn communication take place
    private static double key = (Math.random() * 100000) + 1;               //Generating random secret key for the device
    static BusAttachment mBus;                                              // Alljoyn bus attachment which handles all call backs and connection to Alljoyn bus
    static int mUseSessionId = -1;                                          // the alljoyn session id for the connection
    private static final String NAME_PREFIX = "org.alljoyn.bus.samples.chat";   //The name prefix via which the channels are searched on the network
    //////Variables realted to alljoyn connection establishment

    static int channel_joined = -1;
    static int channel_detected = -1;

    private static ArrayList<String> channels;                              //Arraylist for storing all the visible Alljoyn Channel
    private static ArrayList<String> notification_received;                 //ArrayList for storing current call notifications
    private static ArrayList<messageThread> notification_thread;            //ArrayList for storing the notification thread
    
    
    private static ArrayList<String> MissedCalls;
    private static int channel_selected = -1;
    private static double[] keys = new double[100];                         //Array for storing all the keys that the device received
    private static int key_count = 0;
    private static String nickname;                                         //Device nickname that the user has chosen
    private static String alljoynnick;                                      //Device nickanem that Alljoyn provides
    private static Boolean running;
    private static int ask_key_ind = -1;

    //We can change to a single variable wait for android implementation to be complete
    private static boolean validate = false;
    private static boolean validate_copy = false;
    //         
    ///Variables for the various interfaces used for data transfer 
    static ChatInterface myInterface = null;                               //This is the interface through which all of the alljoyn signals are sent
    static SignalInterface mySignalInterface;                              //This the interface through which the alljoyn signal emitter is intiated
    private static ProxyBusObject mProxyObj;                               //This is used for the alljoyn method interface 
    private static GroupInterface mGroupInterface;                         //This is used for the alljoyn method interface 
    static Methodhandler myGroup = new Methodhandler();                    //This is used to handle alljoyn method calls 
    static Join_Channel j1;                                                //This is an object for the Join channel GUI 
    private static Random rand;
    //This list contains error messges from which any one is picked and shown randomly
    private static String[] ErrorList = {"A monkey Threw a wrench in the gears. Please try again",
        "Monkeys are attacking us again. Try again please",
        "Pigs are flying.That seems to be reason for the crash",
        "Looks like our app went for a vacation. Dont worry we shall bring it back.",
        "The flying monkeys are here, we better hide. Dont worry it's only till our reinforcements arrive."};

    ///End of variable Declarations
    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    // This is used to send message/notification response to the calling device
    public static void sendMessage(String s, String uni) throws BusException {
        SignalEmitter emitter = new SignalEmitter(mySignalInterface, uni, mUseSessionId, SignalEmitter.GlobalBroadcast.Off);
        ChatInterface usrInterface = emitter.getInterface(ChatInterface.class);
        usrInterface.Notify(s, nickname, 0);
    }
    
    //for group chat
    public static void send_msg(String message){
        if(myInterface!=null){
        myInterface.send_message(message, nickname);
        }
    }
    
// This method is used to add missed calls to the missed call arraylist
    public static void call_missed(String s, String nick) {
        int ind = notification_received.indexOf(nick);
        notification_received.remove(ind);
        notification_thread.remove(ind);
        MissedCalls.add(nick+" - "+s);
    }
    // The signal interface is used to send data using alljoyn's signals
    public static class SignalInterface implements ChatInterface, BusObject {

        //Signal via which all the notifications are to be sent
        public void Notify(String s, String nickname, double key) throws BusException {

        }

        //Signal via which all the nickname of new users are to be sent
        @Override
        public void nickname(String usrname, String all_unique) throws BusException {

        }

        //Signal via which the Service/Channel creator validates a new users nickname
        @Override
        public void validate(boolean val) throws BusException {

        }

        //Signal via which users can send their private keys to other devices, thus enabling them to receive their notifications
        @Override
        public void sendKey(Double a) throws BusException {

        }

        @Override
        public void askKey(String s) throws BusException {

        }

        @Override
        public void send_message(String message, String nick) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    //The signal handler reads the signals sent to the device by other devices
    public static class Signalhandler {
        //It handles the notification the PC has received
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "Notify")
        public void Notify(String string, String nick, double key1) {

            if (validate_copy) {
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
                        final String f = string;
                        MessageContext ctx = mBus.getMessageContext();
                        String nickname = ctx.sender;
                        String as = nick + " -> " + string;
                        messageThread sas = new messageThread(as, nickname);
                        sas.start();
                        //Incoming notifications' user name and the corresponding gui thread are stored
                        notification_received.add(nick);
                        notification_thread.add(sas);
                        //For Debugging purpose
                        nickname = nickname.substring(nickname.length() - 10, nickname.length());
                        System.out.println(nickname + ": " + string);
                    } else {
                        
                        int ind = notification_received.indexOf(nick);
                        messageThread temp = notification_thread.get(ind);
                        //The notification thread is stopped
                        temp.stop_thread();
                        // If the user rejects or receives the call from the mobile the notification's properties are removed from the list
                        notification_received.remove(ind);
                        notification_thread.remove(ind);
                    }
                }
            }

        }
        
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "nickname")
        public void nickname(String usrname, String all_unique) {
        }
        
        //This signal handler implementation is done only in client side to revceive nickname validation signal form the server
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "validate")
        public void validate(boolean val) {
            validate = val;
        }

        //This signal handler is used to receive the keys sent to the device
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "sendKey")
        public void sendKey(Double a) {
            System.out.println("key recieved");
            if (a == -1.0) {
                JOptionPane.showMessageDialog(null, "The device did not shared it's key");
            } else {
                keys[key_count] = a;
                key_count++;
            }
        }

        //This method handler is used to respond to request from other devices
        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "askKey")
        public void askkey(String name) throws BusException {
            SignalEmitter emitter = new SignalEmitter(mySignalInterface, name, mUseSessionId, SignalEmitter.GlobalBroadcast.Off);
            ChatInterface usrInterface = emitter.getInterface(ChatInterface.class);
            if (JOptionPane.showConfirmDialog(null,
                    "Are you sure to close this window?", "Really Closing?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {

                usrInterface.sendKey(key);
            } else {
                usrInterface.sendKey(-1.0);
            }

        }
        
        //This signal handler, handles the incoming group chat messages
         @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "send_message")
        public void send_message(String message,String nick){
            String dis=nick+" : "+message;
            Chat_message.add_message(dis);
        }
    }

    //The MethodHandler provides implemention for the GroupInterface which contains declarations for alljoyn methods
    public static class Methodhandler implements GroupInterface, BusObject {

        public synchronized String[] getMem() throws BusException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public synchronized String[] getUni() throws BusException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    //The joinChannel GUI calls this method to specify which of the available channels the user selected
    ///////There should be changes here if the GUI validates the usrs input first before passing it on to the back end
    public static void getChannelName(int i) {
        channel_selected = i;
        System.out.println("the channel id is: " + i);
        if (i == -1) {
            channel_selected = -2;
        }
    }

    //This method joins the channel selected by the user
    ///////There should be changes here if the GUI validates the usrs input first before passing it on to the back end
    public static void joinChannel() throws InterruptedException {
        SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);

        Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

        mBus.enableConcurrentCallbacks();
        // method required which would
        short contactPort = CONTACT_PORT;
        //It waits for the user to select a channel
        while (channel_selected == -1 && running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("Program interupted");
                JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            }
        }
        if (channel_selected == -1) {
            return;
        }
         
        String name = channels.get(channel_selected + 1);
        System.out.println(name);
        Status status = mBus.joinSession(NAME_PREFIX + "." + name, contactPort, sessionId, sessionOpts, new SessionListener(){
            public void sessionLost(int sessionId, int reason) {
               JOptionPane.showMessageDialog(null, "The Service has stopped");
                running=false;
            }
        });
        if (status != Status.OK) {
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            System.out.println(status);
            running=false;
            return;
        }
        System.out.println(String.format("BusAttachement.joinSession successful sessionId = %d", sessionId.value));
        mUseSessionId = sessionId.value;
        
        //All the Interfaces required to send the signals and method calls are initialized
        SignalEmitter emitter = new SignalEmitter(mySignalInterface, sessionId.value, SignalEmitter.GlobalBroadcast.Off);
        System.out.println("flag set");
        myInterface = emitter.getInterface(ChatInterface.class);
        mProxyObj = mBus.getProxyBusObject(NAME_PREFIX + "." + name, "/chatService", sessionId.value, new Class<?>[]{GroupInterface.class});
        mGroupInterface = mProxyObj.getInterface(GroupInterface.class);

        channel_joined = 2;
    }

    //This method is used to update the channel list on the join channel GUI
    public static void update_channel() {
        j1.update_list(channels);
    }

    //This method gets the nicknames of all the devices connected on the channel
    public static String[] get_channel_nick() throws BusException {
        return mGroupInterface.getMem();
    }

    //This method is used to set running to the appropriate value, setting it false will stop the program
    public static void set_running(Boolean run) {
        running = run;
    }

    //This methods sets the user chosen nickname of the device
    public static void set_nick(String nick) {
        nickname = nick;
    }

    //This method is used to set the device index from which key is to be requested
    public static void set_ask_key_ind(int ind) {
        ask_key_ind = ind;
    }

    //This method replies to other devices who have asked the device for their key
    public static void ask_key() throws BusException, InterruptedException {
        ask_key_ind = -1;
        String[] uni_names = mGroupInterface.getUni();
        final String[] nick = mGroupInterface.getMem();
        System.out.println("it's called");
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AskKey(nick, 2, nickname).setVisible(true);
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
            usrInterface.askKey(alljoynnick);

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
     * This method run the code for the client side The process is a as follow:-
     * Initially a Alljoyn busattachment object is initialized this object
     * connects to the Alljoyn bus daemon(it handles the data transfers). Next a
     * buslistener object is initialized, this object listens for data on the
     * channel and call appropriate callback functions when there is any change
     * on the channel. Now all the interface/handlers created are registered
     * with the busattachment. The Signal/method interface and method handler
     * are registered using RegisterBusObject and Signal handler using the
     * RegisterSignalHandler method. Now channel Discovery is initiated so for
     * all the available channels buslistener object's foundAdvertisedname is
     * called. All the found channels are stored in channels array. This array
     * is passed onto the Join channel GUI. The GUI returns the index of the
     * selected channel. Once a channel is found joinChannel method is called,
     * this method blocks till the user has selected a channel After the user
     * has joined a channel he/she is asked for a nickname. This nickname is
     * validated from the channel creator using the nickname and validate
     * methods in the interface. When the user sends a nick to server it checks
     * whether or not the nick is used correspondingly it sends a true or false
     * using the validate method.
     *
     * After all this when call notification is send to the Desktop Notify
     * method on the SignalHandler is called which sends the string to the gui
     * for displaying the notification. When Reject call is pressed a string
     * "bomb" is sent back to device which sent the notification.
     *
     */
    public static void run_client(Boolean run) throws BusException, InterruptedException, FileNotFoundException, UnsupportedEncodingException {
        //Static variables are initialized so as to flush out any residual value from previous run
        running = run;
        channels = new ArrayList<String>();
        MissedCalls = new ArrayList<String>();
        channels.add("nan");
        for (int i = 0; i < 100; i++) {

            keys[i] = -1;
        }
        
        channel_selected = -1;
        myInterface = null;
        mGroupInterface = null;
        j1 = new Join_Channel(channels);
        nickname = null;
        rand = new Random();
        
        class MyBusListener extends BusListener {

            //This method is called whenever the listener discovers a new channel on the network
            public void foundAdvertisedName(String name, short transport, String namePrefix) {
                System.out.println(String.format("BusListener.foundAdvertisedName(%s, %d, %s)", name, transport, namePrefix));
                channels.add(name.substring(29));

                channel_detected = 1;
            }

            public void nameOwnerChanged(String busName, String previousOwner, String newOwner) {
                if ("com.my.well.known.name".equals(busName)) {
                    System.out.println("BusAttachement.nameOwnerChagned(" + busName + ", " + previousOwner + ", " + newOwner);
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

        mBus = new BusAttachment("org.alljoyn.bus.sample.chat", BusAttachment.RemoteMessage.Receive);

        BusListener listener = new MyBusListener();
        mBus.registerBusListener(listener);

        Status status = mBus.connect();
        if (status != Status.OK) {
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            return;
        }
        System.out.println("BusAttachment.connect successful");

        mySignalInterface = new SignalInterface();
        status = mBus.registerBusObject(mySignalInterface, "/chatService");
        status = mBus.findAdvertisedName(NAME_PREFIX);
        if (status != Status.OK) {
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);

            return;
        }

        System.out.println("BusAttachment.findAdvertisedName successful " + "com.my.well.known.name");
        Signalhandler mySignalHandlers = new Signalhandler();

        status = mBus.registerSignalHandlers(mySignalHandlers);
        if (status != Status.OK) {
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);

            return;
        }

        System.out.println("BusAttachment.registerSignalHandlers successful");

        Methodhandler mySampleService = new Methodhandler();

        status = mBus.registerBusObject(mySampleService, "/chatService");
        if (status != Status.OK) {
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);

            return;
        }
        System.out.println("Method handler Registered");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.out.println("Program interupted");
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                j1.setVisible(true);
            }
        });
        joinChannel();
        System.out.println("join channel");
        if (channel_selected == -2) {
            System.out.println("gugugu");
            JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);

            return;
        }
        while (channel_joined != 2 && running) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Program interupted");
                JOptionPane.showMessageDialog(null, ErrorList[rand.nextInt(5)]);
            }
        }

        // Channels Joined
        alljoynnick = mBus.getUniqueName();
        if (running) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new NickName().setVisible(true);
                }
            });
        }
        while (nickname == null && running) {
            Thread.sleep(500);
        }
        

        if (running) {
            myInterface.nickname(nickname, alljoynnick);
            App.set_channel_nickname(channels.get(channel_selected + 1), nickname);
        }

        validate_copy = true;
        System.out.println("Client running");

        if (!running) {
            System.out.println("Client exiting");
            mBus.disconnect();
            App.on_close();
            return;
        }

        String[] uni_names = mGroupInterface.getUni();
        String[] nick = mGroupInterface.getMem();
        for (int i = 0; i < nick.length; i++) {
            System.out.println(uni_names[i] + " - " + nick[i]);
        }
        //This is for the client to run infinetly 
        while (true & running) {
            Thread.sleep(5000);
        }
        System.out.println("Client exiting");
        mBus.disconnect();
        App.on_close();
    }

    public static void main(String[] args) throws BusException, InterruptedException {

    }
}

//This class creates a new thread on which a new jFrame is created for displaying the incoming notification
class messageThread extends Thread {

    final String f;
    final String all_uni;
    private static ChatFrame ch;

    public messageThread(String s, String uni) {
        f = s;
        all_uni = uni;

    }

    public void stop_thread() {
        ch.dispose();
    }

    public void run() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ch = new ChatFrame(f, all_uni, 2);
                ch.setVisible(true);
            }
        });
    }
}
