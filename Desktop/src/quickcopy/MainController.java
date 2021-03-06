/*
 * Copyright 2019 Chipleo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package quickcopy;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Ellipse;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.imageio.ImageIO;
import quickcopy.Themes.Circles;
import quickcopy.Themes.modern;

/**
 * FXML Controller class
 *
 * @author Leonardo Puzzuoli
 */
public class MainController implements Initializable {

    @FXML
    private static DatagramSocket socket = null;
    List<InetAddress> channels;
    Scene scene;
    PServer server = new PServer(this);
    TServer TCPServer = new TServer(4446, this);
    private List<String> myIPs = new ArrayList<>();
    String myname = "QuickCopy";
    static int myport = 4446;
    Ellipse topselector, middleselector, bottomselector, trafficselector;
    AnchorPane settingspane, scannerpane, packpane, welcome_pane, trafficpane;
    long timesince = 0;
    public ThemeInterface theme;
    static private List<Connection> connections = new ArrayList<>();
    Preferences prefs;
    Stage stage;
    public static String os = "Undetected/failure";
    @FXML
    VBox scanlist;
    String[] args;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //settings
        //test loading font for mac (and maybe Linux)
        //Font.loadFont(QuickCopy.class.getResource("/fonts/AirbusISIS.ttf").toExternalForm(), 10);
        //detect os
        System.out.println("Detecting OS");
        String _os_toDetect = System.getProperty("os.name").toLowerCase();
        if (_os_toDetect.contains("win")) {
            os = "Windows";
        }
        if (_os_toDetect.contains("nux")) {
            os = "Linux";
        }
        if (_os_toDetect.contains("mac")) {
            os = "Mac";
        }

        prefs = Preferences.userRoot().node(this.getClass().getName());
        //if first launch
        if (prefs.get("flaunch", "true").equals("true")) {
            System.out.println("First Launch");
            //set shell context
            if ("Windows".equals(os)) {
                //TODO:here
            }

        }
        //get theme
        String theme_selector = prefs.get("theme", "modern_green");

        //choose correct one
        switch (theme_selector) {
            case "circles":
                theme = new Circles();
                break;
            case "modern_green":
                theme = new modern("green");
                break;
            case "modern_yellow":
                theme = new modern("yellow");
                break;
            case "modern_aqua":
                theme = new modern("aqua_blue");
                break;
            default:
                theme = new modern("green");
                break;

        }
        //get username
        myname = prefs.get("username", "QuickCopy");
        //if userame is base64 encoded decode it
        if (!myname.equals("QuickCopy")) {
            byte[] decodedBytes = Base64.getDecoder().decode(myname);
            myname = new String(decodedBytes);
        }

        //get all IP addresses
        try {
            //list of addresses
            List<String> addresses = new ArrayList<>();
            InetAddress localhost = InetAddress.getLocalHost();
            System.out.println(" IP Addr: " + localhost.getHostAddress());
            addresses.add(localhost.getHostAddress());

            // Just in case this host has multiple IP addresses....
            InetAddress[] allMyIps = InetAddress.getAllByName(localhost.getCanonicalHostName());
            if (allMyIps != null && allMyIps.length > 1) {
                System.out.println(" Full list of IP addresses:");
                for (int i = 0; i < allMyIps.length; i++) {
                    System.out.println("    " + allMyIps[i]);
                    addresses.add(allMyIps[i].toString().split("/")[1]);
                    System.out.println(allMyIps[i].toString().split("/")[1]);

                }
            }

            //get IP and Hostname to use as name
            for (String temp : addresses) {
                //if address is correct one
                System.out.println("possible address: " + temp);
                if (temp.startsWith("192.168")) {
                    //set it as my ip
                    if (!myIPs.contains(temp)) {
                        myIPs.add(temp);
                    }
                }
            }
        } catch (UnknownHostException e) {
            System.out.println(" (error retrieving server host name)");
        }

        //start updateConnections
        updateConnections();
    }

    private void broadcast_scan() {
        //do this on new Thread in order not to block UI
        new Thread() {
            @Override
            public void run() {

                //Find All Broadcast Addresses
                System.out.println("STARTING QUERY");
                timesince = System.nanoTime() / 1000000000 - timesince;
                try {
                    channels = listAllBroadcastAddresses();
                } catch (SocketException e) {
                    System.out.println("SocketException: " + e.toString());
                }
                System.out.println("QUERY OVER");
                //Broadcast "QC at IP:port" to all Addresses
                for (int i = 0; i < channels.size(); i++) {
                    System.out.println("BroadCasting on " + channels.get(i).toString().substring(1));
                    //do it for all addresses
                    for (String addr : myIPs) {
                        try {
                            System.out.println("-> QC at " + addr + ":" + myport);
                            broadcast("QC at " + addr + ":" + myport, InetAddress.getByName(channels.get(i).toString().substring(1)));
                        } catch (IOException e) {
                            System.out.println("ERROR: " + e.toString());
                        }
                    }

                }
            }
        }.start();
    }

    @FXML
    private void Scan() {
        //When Scan Icon is Clicked

        topselector.setVisible(true);
        middleselector.setVisible(false);
        bottomselector.setVisible(false);
        trafficselector.setVisible(false);

        scannerpane.setVisible(true);
        packpane.setVisible(false);
        settingspane.setVisible(false);
        welcome_pane.setVisible(false);
        trafficpane.setVisible(false);

        //if Scan has been a long time ago or never happened
        if (timesince < (10 * 60) && timesince != 0) {
            //drawConnections();
        } else {
            connections.clear();

            broadcast_scan();
        }

        //We should log Sys.out/
    }

    void showSpotlight(Connection c) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Parent root;
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("fastSend.fxml"));
                    Pane temp_root = (Pane)loader.load();
                    FastSendController fsc = loader.getController();
                    Stage stage = new Stage();
                    stage.setTitle("SendFast");
                    Scene temp_scene = new Scene(temp_root);
                    stage.setScene(temp_scene);
                    stage.initStyle(StageStyle.TRANSPARENT);
                    fsc.send(c,stage);
                    scene.setFill(null);
                    temp_scene.setFill(null);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void updateConnections() {
        //run broadcast_scan every 3 minutes
        new Thread() {
            @Override
            public void run() {
                //if Thred.sleep() fails multiple times (5) stop autoscanning
                byte interruptions = 0;
                while (true) {
                    //start scan
                    broadcast_scan();
                    //wait for scan to finish
                    try {
                        Thread.sleep(1000 * 10);
                    } catch (InterruptedException e) {

                    }
                    //update TrayIcon connections
                    //Add exit option
                    java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
                    exitItem.addActionListener(event -> {
                        power();
                    });
                    //Add button for each connection
                    final java.awt.PopupMenu popup = new java.awt.PopupMenu();
                    for (Connection c : connections) {
                        //set name to Connection
                        java.awt.MenuItem openItem = new java.awt.MenuItem(c.getName());
                        //set font to BOLD
                        java.awt.Font defaultFont = java.awt.Font.decode(null);
                        java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
                        openItem.setFont(boldFont);
                        //Add eventListener to show message box
                        openItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                showSpotlight(c);
                            }
                        });
                        popup.add(openItem);
                    }
                    //create and set new Popupmenu
                    popup.addSeparator();
                    popup.add(exitItem);
                    trayIcon.setPopupMenu(popup);
                    System.out.println("Updated TrayIcon");
                    try {
                        Thread.sleep(1000 * 60 * 3);
                        interruptions = 0;
                    } catch (InterruptedException e) {
                        if (interruptions >= 5) {
                            break;
                        }
                        interruptions++;
                    }
                }
            }
        }.start();
    }

    public void send(List<String> filePATHS, boolean toEndAfterwards) {
        //filePATHS are the paths to the files to send, Connection is the user to send it to, toEndAfterwards is if the program should colse after calling function is finished

        //open quick menu for sending
        start(stage);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("send.fxml"));
        try {

            Pane bar = (Pane) loader.load();
            barController n = loader.getController();
        } catch (IOException e) {
            System.out.println("Could not load bar out of FXML,: " + e.toString());
        }

    }

    public void start(final Stage primaryStage) {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("send.fxml"));
        try {

            Pane bar = (Pane) loader.load();
            barController n = loader.getController();
            Scene dialogScene = new Scene(bar, 300, 200);
            dialog.setScene(dialogScene);
            dialog.show();
        } catch (IOException e) {
            System.out.println("Could not load bar out of FXML,: " + e.toString());
        }
    }

    void saywhat() {
        System.out.println("what");
    }

    void show() {
        stage.sizeToScene();
        stage.show();
    }

    //a restart is required if theme color is changed
    private boolean restart_required = false;

    @FXML
    TextField nameField;

    @FXML
    void selectBlue() {
        restart_required = true;
        prefs.put("theme", "modern_aqua");
    }

    @FXML
    void selectYellow() {
        restart_required = true;
        prefs.put("theme", "modern_yellow");
    }

    @FXML
    void selectGreen() {
        restart_required = true;
        prefs.put("theme", "modern_green");
    }

    @FXML
    void saveSettings() {
        if (restart_required) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "A restart is required");
            alert.showAndWait();
        }
        String new_username = nameField.getText();
        if (!new_username.equals("")) {
            new_username = Base64.getEncoder().encodeToString(new_username.getBytes());
            prefs.put("username", new_username);
        }
        power();
    }

    @FXML
    CheckBox visible;

    @FXML
    void changeVisibility() {
        System.out.println(visible.isSelected());
        if (visible.isSelected()) {
            server.start();
        } else {
            server.halt();
        }
    }

    private static final String imageLoc
            = "./src/quickcopy/images/temp_ico.png";

    java.awt.SystemTray tray;
    public static java.awt.TrayIcon trayIcon;

    private void addAppToTray() {
        try {
            // ensure awt toolkit is initialized.
            java.awt.Toolkit.getDefaultToolkit();

            // app requires system tray support, just exit if there is no support.
            if (!java.awt.SystemTray.isSupported()) {
                System.out.println("No system tray support, application exiting.");
                Platform.exit();
            }

            // set up a system tray icon.
            tray = java.awt.SystemTray.getSystemTray();
            File file = new File(imageLoc);
            java.awt.Image image = ImageIO.read(file);
            trayIcon = new java.awt.TrayIcon(image);

            // if the user double-clicks on the tray icon, show the main app stage.
            trayIcon.addActionListener(event -> Platform.runLater(this::show));

            // if the user selects the default menu item (which includes the app name),
            // show the main app stage.
            java.awt.MenuItem openItem = new java.awt.MenuItem("hello, world");
            openItem.addActionListener(event -> Platform.runLater(this::saywhat));

            // the convention for tray icons seems to be to set the default icon for opening
            // the application stage in a bold font.
            java.awt.Font defaultFont = java.awt.Font.decode(null);
            java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
            openItem.setFont(boldFont);

            // to really exit the application, the user must go to the system tray icon
            // and select the exit option, this will shutdown JavaFX and remove the
            // tray icon (removing the tray icon will also shut down AWT).
            java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
            exitItem.addActionListener(event -> {
                power();
            });

            // setup the popup menu for the application.
            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            // add the application tray icon to the system tray.
            tray.add(trayIcon);
        } catch (java.awt.AWTException | IOException e) {
            System.out.println("Unable to init system tray");
            e.printStackTrace();
        }
    }

    public void broadcast(
            String broadcastMessage, InetAddress address) throws IOException {
        socket = new DatagramSocket();
        socket.setBroadcast(true);

        byte[] buffer = broadcastMessage.getBytes();

        DatagramPacket packet
                = new DatagramPacket(buffer, buffer.length, address, 4445);
        socket.send(packet);
        socket.close();
    }

    //Find All BroadcastAddresses
    static public List<InetAddress> listAllBroadcastAddresses() throws SocketException {
        List<InetAddress> broadcastList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces
                = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            networkInterface.getInterfaceAddresses().stream()
                    .map(a -> a.getBroadcast())
                    .filter(Objects::nonNull)
                    .forEach(broadcastList::add);
        }
        return broadcastList;
    }

    @FXML
    private void PackMan() {
        topselector.setVisible(false);
        middleselector.setVisible(true);
        bottomselector.setVisible(false);
        trafficselector.setVisible(false);

        scannerpane.setVisible(false);
        packpane.setVisible(true);
        settingspane.setVisible(false);
        welcome_pane.setVisible(false);
        trafficpane.setVisible(false);
    }

    @FXML
    private void Settings() {
        topselector.setVisible(false);
        middleselector.setVisible(false);
        bottomselector.setVisible(true);
        trafficselector.setVisible(false);

        scannerpane.setVisible(false);
        packpane.setVisible(false);
        settingspane.setVisible(true);
        welcome_pane.setVisible(false);
        trafficpane.setVisible(false);
    }

    @FXML
    private void Traffic() {
        topselector.setVisible(false);
        middleselector.setVisible(false);
        bottomselector.setVisible(false);
        trafficselector.setVisible(true);

        scannerpane.setVisible(false);
        packpane.setVisible(false);
        settingspane.setVisible(false);
        welcome_pane.setVisible(false);
        trafficpane.setVisible(true);
    }

    @FXML
    private void disc(MouseEvent event) {
        //check actual state
        boolean isToggled = false;

        //turn server on/off
        if (isToggled) {
            server.halt();
        } else {
            server.start();
        }
    }

    @FXML
    private void power() {
        //halt application
        server.halt();
        TCPServer.halt();
        Platform.exit();
        tray.remove(trayIcon);
        System.exit(0);
    }

    private void power(boolean a) {
        //halt application
        //server.halt();
        //TCPServer.halt();
        Platform.exit();
        //tray.remove(trayIcon);
        System.exit(0);
    }

    public void sendScene(Scene scene_l, Stage s, String[] args) {
        //does an instance of this program already exist on this omputer?
        boolean instanceRunning = true;
        //try to connect to own socket to see if a connection is possible
        TClient tc = new TClient();
        try {
            tc.startConnection(myIPs.get(0), myport);
        } catch (SocketTimeoutException e) {
            instanceRunning = false;
            System.out.println("Single instance detected");
        }
        if (instanceRunning) {
            System.out.println("Multiple Instances of program detected");
            //does the program have CLArguments?
            if (args.length > 1) {
                System.out.println(args);
                //send files after user selects destination
                send(Arrays.asList(args), true);
            } else {
                power(true);
            }
        }
        //start all
        System.out.println(myIPs);
        server.setIP(myIPs);
        server.setHostname(myname);
        server.start();
        TCPServer.start();
        server.setPort(myport);

        //add SystemTray ico
        // instructs the javafx system not to exit implicitly when the last application window is shut.
        Platform.setImplicitExit(false);
        // sets up the tray icon (using awt code run on the swing thread).
        javax.swing.SwingUtilities.invokeLater(this::addAppToTray);

        scene = scene_l;
        stage = s;
        //get all interactables

        topselector = (Ellipse) scene.lookup("#selectorontop");
        middleselector = (Ellipse) scene.lookup("#selectoronmiddle");
        bottomselector = (Ellipse) scene.lookup("#selectoronbottom");
        trafficselector = (Ellipse) scene.lookup("#trafficselector");

        settingspane = (AnchorPane) scene.lookup("#settings");
        scannerpane = (AnchorPane) scene.lookup("#scanner");
        packpane = (AnchorPane) scene.lookup("#packets");
        welcome_pane = (AnchorPane) scene.lookup("#Welcome");
        trafficpane = (AnchorPane) scene.lookup("#traffic");

        setdrawPackages();

        //get home page
        String home = prefs.get("home", "Welcome");
        switch (home) {
            case "Welcome":
                topselector.setVisible(false);
                middleselector.setVisible(false);
                bottomselector.setVisible(false);
                trafficselector.setVisible(false);

                scannerpane.setVisible(false);
                packpane.setVisible(false);
                settingspane.setVisible(false);
                welcome_pane.setVisible(true);
                trafficpane.setVisible(false);
                break;
            case "Scan":
                Scan();
                break;
            case "Package":
                PackMan();
                break;
            case "Traffic":
                Traffic();
                break;
            case "Settings":
                Settings();
                break;
            default:
                break;
        }

    }

    public synchronized static void addConnection(Connection conn, MainController contr) {
        boolean _found = false;
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).getAddr().equals(conn.getAddr())) {
                _found = true;
                break;
            }
        }
        if (!_found) {
            connections.add(conn);
            System.out.println("connection added");
            //only draw Conns if new conn is added
            contr.drawConnections();
        }

    }

    public static void setMyPort(int port) {
        myport = port;
    }

    @FXML
    private void drawConnections() {
        theme.draw(connections, scannerpane, scanlist);
    }

    public static List getConnections() {
        return connections;
    }

    @FXML
    private void setdrawPackages() {
        boolean darkmode = false;
        //draw PackMan
        FXMLLoader loader = new FXMLLoader();
        //if darkmode, then load the darkmode version
        if (darkmode) {
            loader.setLocation(getClass().getResource("package_dark.fxml"));
        } else {

            loader.setLocation(getClass().getResource("package.fxml"));
        }

        try {
            ScrollPane showBoxes = (ScrollPane) loader.load();
            packpane.getChildren().add(showBoxes);
            //send scene so PMC can add to VBox
            PackageManagerController controller = (PackageManagerController) loader.getController();
            controller.sendScene(showBoxes);
        } catch (IOException e) {
            System.out.println("The boxes screen failed to load: " + e.toString());
        }
    }
}
