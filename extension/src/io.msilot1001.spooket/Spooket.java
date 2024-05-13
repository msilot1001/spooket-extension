package io.msilot1001.spooket;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import android.app.Activity;
import android.os.StrictMode;
import android.util.Log;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.AsynchUtil;

@DesignerComponent(
        description =
                "Socket.io java client in App Inventor 2 by Msilot1001",
        category = ComponentCategory.EXTENSION,
        helpUrl = "https://github.com/msilot1001/socketIO-extension",
        nonVisible = true,
        version = 1,
        versionName = "1.0.0"
)
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@SimpleObject(external = true)
public class Spooket extends AndroidNonvisibleComponent {
    private static final String LOG_TAG = "Spooket";

    private final Activity activity;

    // the socket object
    private Socket clientSocket = null;
    // the address to connect to
    private String serverAddress = "";
    // the port to connect to
    private String serverPort = "";
    // connection timeout to server in ms
    private int timeoutms = 5000;
    // boolean that indicates the state of the connection, true = connected, false = not connected
    private boolean connectionState = false;
    // boolean that indicates the mode used, false = string sent as is, true = String is considered as hexadecimal data and will be converted before sending
    // same behavior is used when receiving data
    private boolean hexaStringMode = false;
    // boolean to enable debug messages "YailRuntimeError" (true by default to ensure same behavior as before)
    private boolean debugMessages = true;

    private int clientIdentifier = 00;

    InputStream inputStream = null;

    /**
     * Creates a new Client Socket component.
     *
     * @param container the Form that this component is contained in.
     */
    public Spooket(ComponentContainer container) {
        super(container.$form());
        activity = container.$context();
        // compatibility with AppyBuilder (thx Hossein Amerkashi <kkashi01 [at] gmail [dot] com>)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    //region Properties

    /**
     * Method that returns the server's address.
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The address of the server the client will connect to.")
    public String ServerAddress() {
        return serverAddress;
    }

    /**
     * Method to specify the server's address
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "192.168.1.213")
    @SimpleProperty
    public void ServerAddress(String address) {
        serverAddress = address;
    }

    /**
     * Method that returns the server's port.
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The port of the server the client will connect to.")
    public String ServerPort() {
        return serverPort;
    }

    /**
     * Method to specify the server's port
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "25565")
    @SimpleProperty
    public void ServerPort(String port) {
        serverPort = port;
    }

    /**
     * Method that returns the timeout to server in ms
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The timeout to server in ms")
    public int TimeoutMs() {
        return timeoutms;
    }

    /**
     * Method to specify the timeout to server in ms
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "2000")
    @SimpleProperty
    public void TimeoutMs(int timeout) {
        timeoutms = timeout;
    }

    /**
     * Method that returns the connection state
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The state of the connection - true = connected, false = disconnected")
    public boolean ConnectionState() {
        return connectionState;
    }

    /**
     * Method that returns a string containing "\n\r" sequence
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "returns a string containing \"\n\r\" sequence")
    public String SeqNewLineAndRet() {
        String seq = "";
        seq = seq + '\n' + '\r';
        return seq;
    }

    /**
     * Method that returns a string containing "\r\n" sequence
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "returns a string containing \"\r\n\" sequence")
    public String SeqRetAndNewLine() {
        String seq = "";
        seq = seq + '\r' + '\n';
        return seq;
    }

    /**
     * Method that returns a string containing "\r" sequence
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "returns a string containing \"\r\" sequence")
    public String SeqRet() {
        String seq = "";
        seq = seq + '\r';
        return seq;
    }

    /**
     * Method that returns a string containing "\n" sequence
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "returns a string containing \"\n\" sequence")
    public String SeqNewLine() {
        String seq = "";
        seq = seq + '\n';
        return seq;
    }

    /**
     * Method that returns the mode (string or hexastring)
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The mode of sending and receiving data.")
    public boolean HexaStringMode() {
        return hexaStringMode;
    }

    /**
     * Method to specify the mode (string or hexastring)
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN)
    @SimpleProperty
    public void HexaStringMode(boolean mode) {
        hexaStringMode = mode;
    }

    /**
     * Method that returns the display of debug messages
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "The display of debug messages.")
    public boolean DebugMessages() {
        return debugMessages;
    }

    /**
     * Method to specify the display of debug messages
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void DebugMessages(boolean displayDebugMessages) {
        debugMessages = displayDebugMessages;
    }
    //endregion


    /**
     * Creates the socket, connect to the server and launches the thread to receive data from server
     */
    @SimpleFunction(description = "Tries to connect to the server and launches the thread for receiving data (blocking until connected or failed)")
    public void Connect() {
        if (connectionState) {
            if (debugMessages)
                throw new YailRuntimeError("Connect error, socket connected yet, please disconnect before reconnect !", "Error");
        }
        try {
            // connecting the socket
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(serverAddress, Integer.parseInt(serverPort)), timeoutms);
            connectionState = true;
            // begin the receive loop in a new thread
            AsynchUtil.runAsynchronously(new Runnable() {
                @Override
                public void run() {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                    byte[] buffer = new byte[1024];
                    int bytesRead = 0;

                    try {
                        // get the input stream and save the data
                        inputStream = clientSocket.getInputStream();
                        while (true) {
                            // test if there is a server problem then close socket properly (thx Axeley :-))
                            try {
                                bytesRead = inputStream.read(buffer);
                                if (bytesRead == -1) {
                                    connectionState = false;
                                    break;
                                }
                            } catch (SocketException e) {
                                //throw e;
                            } catch (IOException e) {
                                if (e.getMessage().indexOf("ETIMEDOUT") >= 0)
                                    break;
                                //throw e;
                            }

                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                            final String dataReceived;
                            // hexaStringMode is false, so we don't transform the string received
                            if (!hexaStringMode) {
                                dataReceived = byteArrayOutputStream.toString("UTF-8");
                            }
                            // hexaStringMode is true, so we make a string with each character as an hexa symbol representing the received message
                            else {
                                int i;
                                char hexaSymbol1, hexaSymbol2;
                                String tempData = "";
                                byte[] byteArray = byteArrayOutputStream.toByteArray();
                                for (i = 0; i < byteArrayOutputStream.size(); i++) {
                                    if (((byteArray[i] & 0xF0) >> 4) < 0xA)
                                        // 0 to 9 symbol
                                        hexaSymbol1 = (char) (((byteArray[i] & 0xF0) >> 4) + 0x30);
                                    else
                                        // A to F symbol
                                        hexaSymbol1 = (char) (((byteArray[i] & 0xF0) >> 4) + 0x37);
                                    if ((byteArray[i] & 0x0F) < 0xA)
                                        hexaSymbol2 = (char) ((byteArray[i] & 0x0F) + 0x30);
                                    else
                                        hexaSymbol2 = (char) ((byteArray[i] & 0x0F) + 0x37);
                                    tempData = tempData + hexaSymbol1 + hexaSymbol2;
                                }

                                dataReceived = tempData;
                            }
                            // reset of the byteArrayOutputStream to flush the content
                            byteArrayOutputStream.reset();
                            // then we send the data to the user using an event
                            // events must be sent by the main thread (UI)
                            final Packet packet = new Packet(dataReceived.substring(0,4), dataReceived.substring(4,1022),dataReceived.substring(1022));
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DataReceived(packet);
                                }
                            });
                        }
                        // When we go there, either we have
                        // - server shutdown
                        // - disconnection asked (inputstream closed => -1 returned)
                        // - connection problem
                        // so, if it is not disconnected yet, we disconnect the socket and inform the user of it.
//                        if (connectionState) {
//                            Disconnect();
//                            // events must be sent by the main thread (UI)
//                            activity.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    RemoteConnectionClosed();
//                                }
//                            });
//                        }
                    } catch (SocketException e) {
                        Log.e(LOG_TAG, "ERROR_READ", e);
                        if (debugMessages)
                            throw new YailRuntimeError("Connect error (read 1) " + e.getMessage(), "Error");
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "ERROR_READ", e);
                        if (debugMessages)
                            throw new YailRuntimeError("Connect error (read 2)", "Error");
                    } catch (Exception e) {
                        connectionState = false;
                        Log.e(LOG_TAG, "ERROR_READ", e);
                        if (debugMessages)
                            throw new YailRuntimeError("Connect error (read 3) " + e.getMessage(), "Error");
                    }
                }
            });
        } catch (SocketException e) {
            Log.e(LOG_TAG, "ERROR_CONNECT", e);
            if (debugMessages)
                throw new YailRuntimeError("Connect error" + e.getMessage(), "Error");
        } catch (Exception e) {
            connectionState = false;
            Log.e(LOG_TAG, "ERROR_CONNECT", e);
            if (debugMessages)
                throw new YailRuntimeError("Connect error (Socket Creation, please check Ip or hostname -> )" + e.getMessage(), "Error");
        }
    }

    /**
     * Send data through the socket to the server
     */
    public void SendData(final byte[] data) {
        if (!connectionState) {
            if (debugMessages)
                throw new YailRuntimeError("Send error, socket not connected.", "Error");
        }
        final byte[] dataToSend = data;
//        byte[] dataCopy = data;
//        if (!hexaStringMode) {
//            //dataSend = new byte [data.length()];
//            // if hexaStringMode is false, we send data as is
//            dataToSend = dataCopy;
//        }
//        else {
//            // if hexaStringMode is true, we begin to verify we can transcode the symbols
//            // verify if the data we want to send contains only hexa symbols
//            int i;
//            for (i = 0; i < data.length(); i++) {
//                if (((dataCopy[i] < 0x30) || (dataCopy[i] > 0x39)) && ((dataCopy[i] < 0x41) || (dataCopy[i] > 0x46)) && ((dataCopy[i] < 0x61) || (dataCopy[i] > 0x66)))
//                    if (debugMessages)
//                        throw new YailRuntimeError("Send data : hexaStringMode is selected and non hexa symbol found in send String.", "Error");
//            }
//            // verify that the number of symbols is even
//            if ((data.length() % 2) == 1) {
//                if (debugMessages)
//                    throw new YailRuntimeError("Send data : hexaStringMode is selected and send String length is odd. Even number of characters needed.", "Error");
//            }
//            // if all tests pass, we transcode the data :
//            dataToSend = new byte[data.length() / 2];
//            for (i = 0; i < data.length(); i = i + 2) {
//                byte[] temp1 = new byte[2];
//                temp1[0] = dataCopy[i];
//                temp1[1] = dataCopy[i + 1];
//                String temp2 = new String(temp1);
//                dataToSend[i / 2] = (byte) Integer.parseInt(temp2, 16);
//            }
//            // those two lines were removed because of a bug sending trailing 0x00 in hexaString mode
//            // if 0x00 is really needed, we should add it directly in the string we want to send
//            // end of c-type string character
//            // dataToSend[i/2] = (byte)0x00;
//        }

        // we then send asynchronously the data
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream out;
                    out = clientSocket.getOutputStream();
                    out.write(dataToSend);
                } catch (SocketException e) {
                    Log.e(LOG_TAG, "ERROR_SEND", e);
                    if (debugMessages)
                        throw new YailRuntimeError("Send data" + e.getMessage(), "Error");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "ERROR_UNABLE_TO_SEND_DATA", e);
                    if (debugMessages)
                        throw new YailRuntimeError("Send Data", "Error");
                }
            }
        });
    }

    /**
     * Close the socket
     */
    @SimpleFunction(description = "Disconnect to the server")
    public void Disconnect() {
        if (connectionState) {
            connectionState = false;
            try {
                // shutdown the input socket,
                //clientSocket.shutdownInput();
                //clientSocket.shutdownOutput();
                clientSocket.close();
            } catch (SocketException e) {
                // modifications by axeley too :-)
                if (e.getMessage().indexOf("ENOTCONN") == -1) {
                    Log.e(LOG_TAG, "ERROR_CONNECT", e);
                    if (debugMessages)
                        throw new YailRuntimeError("Disconnect" + e.getMessage(), "Error");
                }
                // if not connected, then just ignore the exception
            } catch (IOException e) {
                Log.e(LOG_TAG, "ERROR_CONNECT", e);
                if (debugMessages)
                    throw new YailRuntimeError("Disconnect" + e.getMessage(), "Error");
            } catch (Exception e) {
                Log.e(LOG_TAG, "ERROR_CONNECT", e);
                if (debugMessages)
                    throw new YailRuntimeError("Disconnect" + e.getMessage(), "Error");
            } finally {
                clientSocket = null;
            }

        } else if (debugMessages)
            throw new YailRuntimeError("Socket not connected, can't disconnect.", "Error");
    }

    /**
     * Event indicating that a message has been received
     *
     * @param data the data sent by the server
     */
    public void DataReceived(Packet data) {
        String type = data.type;
        String payload = data.payload;

        // identify the type of the packet received
        if(type.startsWith("CS")) {
            // greeting
            clientIdentifier = Integer.getInteger(payload.substring(0,2));
            String userid = payload.substring(2,38);
        }
    }

    /**
     * Event indicating that the remote socket closed the connection
     */
    @SimpleEvent
    public void RemoteConnectionClosed() {
        // invoke the application's "RemoteConnectionClosed" event handler.
        EventDispatcher.dispatchEvent(this, "RemoteConnectionClosed");
    }

    @SimpleFunction(description = "request server to join room")
    public void JoinRoom(int RoomId) {
        byte[] packet = createPacket("CR00", Integer.toString(RoomId), "\r\n");
        SendData(packet);
    }

    public void Greeting(String name) {
        byte[] packet = createPacket("CS00", name, "\r\n");
        SendData(packet);
    }

    public byte[] createPacket(String type, String payload, String sEnd) {
        byte[] buffer = new byte[1024];

        char[] typeArr = type.replaceAll("00", Integer.toString(clientIdentifier)).toCharArray();
        char[] sEndArr = sEnd.toCharArray();

        // type
        buffer[0] = (byte) typeArr[0];
        buffer[1] = (byte) typeArr[1];
        buffer[2] = (byte) typeArr[2];
        buffer[3] = (byte) typeArr[3];

        // payload
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        if (payloadBytes.length > 1018) {
            throw new Error("Too big payload!");
        } else System.arraycopy(payloadBytes, 0, buffer, 4, payloadBytes.length);

        // end
        buffer[1022] = (byte)sEndArr[0];
        buffer[1023] = (byte)sEndArr[1];

        return buffer;
    }
}

class Packet {
    public String type;
    public String payload;
    public String sEnd;
    public Packet(String type, String payload, String sEnd) {
        this.type=type;
        this.payload=payload;
        this.sEnd=sEnd;
    }
}