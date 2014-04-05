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

import java.util.ArrayList;
import java.util.Scanner;
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

public class Service {

    static {
        System.loadLibrary("alljoyn_java");
    }

    public static void sendMessage(String s, String uni) throws BusException {
        SignalEmitter emitter = new SignalEmitter(mySignalInterface, uni, mSessionId, SignalEmitter.GlobalBroadcast.Off);
        ChatInterface usrInterface = emitter.getInterface(ChatInterface.class);
        usrInterface.Notification(s, nickname[0], 0);
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
    private static double key = (Math.random() * 100000) + 1;         //Generating random secret key for the device

    private static String[] Alljoyn_unique_name = new String[100];//stores the nicknames provided to devices by alljoyn
    static String[] nickname = new String[100];                   //stores the nicknames chosen by the user
    private static int name_count = 0;

    private static double[] keys = new double[100];               //stores the all the keys it has recieved
    private static int key_count = 0;
    private static ArrayList<String> channels;               //Array for storing all the visible Alljoyn Channel
    private static int channel_count = 0;
    private static Boolean channel_name_checked = false;
    private static Boolean running;
    
    private static ChatInterface myInterface = null;
    private static SignalInterface mySignalInterface;
    static String channel_name = null;
    //End of Variable Declarations

   

    // The signal interface is used to send data using alljoyn's signals
    public static class SignalInterface implements ChatInterface, BusObject {

        //Signal via which all the notifications are to be sent
        public void Notification(String s, String nickname, double key) throws BusException {
        }

        //Signal via which all the nickname of new users are to be sent
        @Override
        public void nickname(String usrname, String Alljoyn_unique_nameque) throws BusException {
        }

        //Signal via which the Service/Channel creator validates a new users nickname
        @Override
        public void validate(boolean val) throws BusException {
        }

        //Signal via which users can send their private keys to other devices, thus enabling them to receive their notifications
        @Override
        public void sendKey(Double a) throws BusException {
        }
    }

    //The signal handler reads the signals sent to the device by other devices
    public static class SignalHandler {

        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "Notification")
        public void Notification(String string, String nick, double key1) {
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
                MessageContext ctx = mBus.getMessageContext();
                String nickname = ctx.sender; //returns the alljoyn unique name of the sender;
                String as = nick + " -> " + string;
                new messageTh(as, nickname).start();

                // for debugging purpose
                nickname = nickname.substring(nickname.length() - 10, nickname.length());
                System.out.println(nickname + ": " + string);
            }

        }

        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "nickname")
        public void nickname(String usrname, String Alljoyn_unique_nameque) throws BusException {
            System.out.println("!!!Validation Called!!!");
            SignalEmitter emitter = new SignalEmitter(mySignalInterface, Alljoyn_unique_nameque, mSessionId, SignalEmitter.GlobalBroadcast.Off);
            ChatInterface usrInterface = emitter.getInterface(ChatInterface.class);
            int do_not_contain = 0;             // 0 is for false and 1 true;
            for (int i = 0; i < 100; i++) {
                if (nickname[i].equals(usrname)) {
                    do_not_contain = 0;
                    break;
                }
                if (nickname[i].equals("")) {
                    do_not_contain = 1;
                    break;
                }

            }
            if (do_not_contain == 1) {
                nickname[name_count] = usrname;
                Alljoyn_unique_name[name_count] = Alljoyn_unique_nameque;
                name_count++;
                usrInterface.validate(true);

            } else {
                usrInterface.validate(false);
            }

        }

        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "validate")
        public void validate(boolean val) {
        }

        @BusSignalHandler(iface = "org.alljoyn.bus.samples.chat", signal = "sendKey")
        public void sendKey(Double a) {
            keys[key_count] = a;
            key_count++;
        }
    }

    //The MethodHandler provides implemention for the GroupInterface which contains declarations for alljoyn methods
    public static class MethodHandler implements GroupInterface, BusObject {

        public void preDispatch() {
        }

        public void postDispatch() {
        }

        //Method via which a device can ask from other device for its key
        @Override
        public synchronized double askKey() {
            return key;
        }

        //Method via which a device can ask from the service/channel creator for the user assigned nicknames of the devices connected 
        @Override
        public synchronized String[] getMem() throws BusException {
            return nickname;
        }

        //Method via which a device can ask from the service/channel creator for the Alljoyn nicknames of the devices connected
        @Override
        public synchronized String[] getUni() throws BusException {
            return Alljoyn_unique_name;
        }

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
                if(channels.contains(channel_name)){
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

    public static void set_running(Boolean run){
        running = run;
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
    public static void run_service(Boolean run) throws BusException {
        running=run;
        channel_count=0;
        key_count=0;
        name_count=0;
        channel_name_checked=false;
        mySignalInterface=null;
        myInterface=null;
        channels = new ArrayList<String>();
      
        //Initializing all the nicknames
        for (int i = 0; i < 100; i++) {
            Alljoyn_unique_name[i] = "";
            nickname[i] = "";
            keys[i] = -1;
            
        }

        //mBus is the object which connects to the Alljoyn bus daemon
        mBus = new BusAttachment("org.alljoyn.bus.samples", BusAttachment.RemoteMessage.Receive);

        Status status;

        mySignalInterface = new SignalInterface();

        status = mBus.registerBusObject(mySignalInterface, "/chatService");
        if (status != Status.OK) {
            return;
        }
        System.out.println("BusAttachment.registerBusObject successful");

        BusListener listener = new MyBusListener();
        mBus.registerBusListener(listener);

        status = mBus.connect();
        if (status != Status.OK) {
            return;
        }
        System.out.println("BusAttachment.connect successful on " + System.getProperty("org.alljoyn.bus.address"));

        SignalHandler mySignalHandlers = new SignalHandler();

        status = mBus.registerSignalHandlers(mySignalHandlers);
        if (status != Status.OK) {

            return;
        }
        System.out.println("Signal Handler registered");

        MethodHandler mySampleService = new MethodHandler();

        status = mBus.registerBusObject(mySampleService, "/chatService");
        if (status != Status.OK) {
            System.out.println(status);
            return;
        }
        System.out.println("Method handler Registered");

        mBus.findAdvertisedName(NAME_PREFIX);
        //Asking the user to set his/her nickname
        Alljoyn_unique_name[name_count] = mBus.getUniqueName();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter a nick name");
        nickname[name_count] = scanner.nextLine();
        name_count++;

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
                    }
                });
        if (status != Status.OK) {
            return;
        }
        System.out.println("BusAttachment.bindSessionPort successful");

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Create_Channel(channels).setVisible(true);
            }
        });
        while (channel_name == null && running) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {

            }
        }

        System.out.println(channel_name);

        String wellKnownName = NAME_PREFIX + "." + channel_name;
        int flags = 0; //do not use any request name flags
        status = mBus.requestName(wellKnownName, flags);
        if (status != Status.OK) {
            return;
        }
        System.out.println("BusAttachment.request 'com.my.well.known.name' successful");

        status = mBus.advertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
        if (status != Status.OK) {
            System.out.println("Status = " + status);
            mBus.releaseName(wellKnownName);
            return;
        }
        System.out.println("BusAttachment.advertiseName 'com.my.well.known.name' successful");

        try {
            while (!mSessionEstablished && running) {
                Thread.sleep(10);

            }
            System.out.println("Server running");

            //This is for the client to run infinetly
            while (true&&running) {

                Thread.sleep(20000);
                myInterface.Notification("service_message", nickname[0], 0);
            }
        } catch (InterruptedException ex) {
            System.out.println("Interrupted");
        }
        System.out.println("Service exiting");
        mBus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
        mBus.disconnect();
        App.on_close();
    }

    public static void main(String[] args) throws BusException {

    }
}

//This class creates a new thread on which a new jFrame is created for displaying the incoming notification
class messageTh extends Thread {

    final String f;
    final String all_uni;

    public messageTh(String s, String uni) {
        f = s;
        all_uni = uni;
    }

    public void run() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ChatFrame(f, all_uni).setVisible(true);
            }
        });
    }
}
