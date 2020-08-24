/* Gaurav Agrawal (ga1380) Final Project */

package finalproject.client;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.sql.*;
import java.util.List;
import javax.swing.*;

import finalproject.db.DBInterface;
import finalproject.entities.Person;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;

public class ClientInterface extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PORT = 8001;

    private static final int FRAME_WIDTH = 600;
    private static final int FRAME_HEIGHT = 400;
    final int AREA_ROWS = 10;
    final int AREA_COLUMNS = 40;

    JComboBox peopleSelect;
    JFileChooser jFileChooser;
    Socket socket;
    int port;

    private JMenuBar menuBar;
    private JPanel dbPanel;
    private JLabel dbName;
    private JPanel connPanel;
    private JPanel comboPanel;
    private JLabel connName;
    private JPanel openCloseBtnPanel;
    private JButton openConnBtn;
    private JButton closeConnBtn;
    private JPanel dataBtnPanel;
    private JButton sendDataBtn;
    private JButton queryDataBtn;
    private JTextArea textArea;
    private JPanel topPanel;

    DBInterface clientDB;


    public ClientInterface() {
        this(DEFAULT_PORT);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        jFileChooser = new JFileChooser(".");
        menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        this.setJMenuBar(menuBar);

        dbPanel = new JPanel();
        dbPanel.add(new JLabel("Active DB: "));
        dbName = new JLabel("<None>");
        dbPanel.add(dbName);

        connPanel = new JPanel();
        connPanel.add(new JLabel("Active Connection: "));
        connName = new JLabel("<None>");
        connPanel.add(connName);

        comboPanel = new JPanel();
        peopleSelect = new JComboBox();
        peopleSelect.addItem("<Empty>");
        comboPanel.add(peopleSelect);

        openCloseBtnPanel = new JPanel();
        openConnBtn = new JButton("Open Connection");
        openConnBtn.addActionListener(new OpenConnectionListener());

        closeConnBtn = new JButton("Close Connection");
        closeConnBtn.addActionListener((e) -> {
            try {
                socket.close();
                textArea.append("Connection closed\n");
                connName.setText("<None>");
            } catch (Exception e1) {
                System.err.println("Connection closing Error: " + e1.getMessage());
            }
        });

        openCloseBtnPanel.add(openConnBtn);
        openCloseBtnPanel.add(closeConnBtn);

        dataBtnPanel = new JPanel();
        sendDataBtn = new JButton("Send Data");
        sendDataBtn.addActionListener(new SendButtonListener()); //Adding action listener to Send Data button
        queryDataBtn = new JButton("Query DB Data");
        queryDataBtn.addActionListener(queryButtonListener); //Adding action listener to Query DB Data button

        dataBtnPanel.add(sendDataBtn);
        dataBtnPanel.add(queryDataBtn);

        textArea = new JTextArea(AREA_ROWS, AREA_COLUMNS); // creating a new textArea
        textArea.setEditable(false); // making the frame non editable

        JScrollPane textAreaScroller = new JScrollPane(textArea); // creating a scroller and adding textArea to it
        //textAreaScroller.setPreferredSize(new Dimension(250, 200));

        topPanel = new JPanel(new GridLayout(5, 1));
        topPanel.add(dbPanel);
        topPanel.add(connPanel);
        topPanel.add(comboPanel);
        topPanel.add(openCloseBtnPanel);
        topPanel.add(dataBtnPanel);

        this.add(topPanel, BorderLayout.NORTH);
        this.add(textAreaScroller);

    }

    public ClientInterface(int port) {
        this.port = port;

    }

    public JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.add(createFileOpenItem());
        menu.add(createFileExitItem());
        return menu;
    }

    private void fillComboBox() throws SQLException {

        List<ComboBoxItem> l = getNames();
        peopleSelect.setModel(new DefaultComboBoxModel(l.toArray()));
    }

    private void clearComboBox() {
        peopleSelect.removeAllItems();
    }

    private JMenuItem createFileOpenItem() {
        JMenuItem item = new JMenuItem("Open DB");
        class OpenDBListener implements ActionListener {
            public void actionPerformed(ActionEvent event) {
                int returnVal = jFileChooser.showOpenDialog(getParent());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    System.out.println("You chose to open this file: " + jFileChooser.getSelectedFile().getAbsolutePath());
                    String dbFileName = jFileChooser.getSelectedFile().getAbsolutePath();
                    try {
                        connectToDB(dbFileName);
                        dbName.setText(dbFileName.substring(dbFileName.lastIndexOf("\\") + 1));
                        queryButtonListener.setConnection(clientDB.getConn());
                        //clearComboBox();
                        fillComboBox();

                    } catch (Exception e) {
                        System.err.println("error connection to db: " + e.getMessage());
                        e.printStackTrace();
                        dbName.setText("<None>");
                        clearComboBox();
                    }

                }
            }
        }

        item.addActionListener(new OpenDBListener());
        return item;
    }


    private JMenuItem createFileExitItem() {
        JMenuItem item = new JMenuItem("Exit");
        item.addActionListener((e) -> System.exit(0));
        return item;
    }

    public void connectToDB(String fileName) {

        try {
            // Connect to a database
            clientDB = new DBInterface();
            Connection conn = DriverManager.getConnection
                    ("jdbc:sqlite:" + fileName);
            clientDB.setConnection(conn);
        } catch (Exception e) {
            System.exit(0);
        }

    }

    ObjectOutputStream sendToServer;
    InputStreamReader fromSever;

    class OpenConnectionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            try {
                socket = new Socket("localhost", port);
                connName.setText(socket.getInetAddress().getHostName() + ":" + socket.getPort());
                sendToServer = new ObjectOutputStream(socket.getOutputStream());
                fromSever = new InputStreamReader(socket.getInputStream());
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                textArea.append("Connection Failure\n");
            }
        }

    }

    class SendButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            try {

                // responses are going to come over the input as text, and that's tricky,
                // which is why I've done that for you:
                // BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader br = new BufferedReader(fromSever);

                // now, get the person on the object dropdownbox we've selected
                ComboBoxItem personEntry = (ComboBoxItem) peopleSelect.getSelectedItem();

                // That's tricky which is why I have included the code. the personEntry
                // contains an ID and a name. You want to get a "Person" object out of that
                // which is stored in the database

                // Send the person object here over an output stream that you got from the socket.
                // Create an output stream to the server

                Integer selectedID = personEntry.getId();
                PreparedStatement stmt = clientDB.getConn().prepareStatement("Select * from People where id=?");
                stmt.setInt(1, selectedID);
                ResultSet rset = stmt.executeQuery();
                Person selectedPerson;
                if (rset.next()) {
                    selectedPerson = new Person(rset.getString("first"), rset.getString("last"), rset.getInt("age"), rset.getString("city"), rset.getInt("id"));
                    sendToServer.writeObject(selectedPerson);
                }

                String response = br.readLine();
                if (response.contains("Success")) {
                    System.out.println("Success");
                    // what do you do after we know that the server has successfully
                    // received the data and written it to its own database?
                    // you will have to write the code for that.

                    stmt = clientDB.getConn().prepareStatement("update People set sent=1 where id=?");
                    stmt.setInt(1, selectedID);
                    stmt.executeUpdate(); //updating the database for th people who are sent successfully
                    fillComboBox(); //rebuilding the JComboBox contents with the latest results in the DB
                } else {
                    System.out.println("Failed");
                }
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                System.err.println("I/O Error in SendButtonListener:" + e1.getMessage());
                textArea.append("Connection to the server is closed. Please open the connection.\n");
            } catch (SQLException e2) {
                // TODO Auto-generated catch block
                System.err.println("SQL Error in SendButtonListener:" + e2.getMessage());
            } catch (Exception e3) {
                // TODO Auto-generated catch block
                System.err.println("Error3 in SendButtonListener:" + e3.getMessage());
                textArea.append("Either the connection is not established or there is no person selected to send.\n");
            }
        }
    }

    QueryButtonListener queryButtonListener = new QueryButtonListener();

    class QueryButtonListener extends DBInterface implements ActionListener {

        public QueryButtonListener() {
            super();
        }

        public void actionPerformed(ActionEvent event) {
            try {

                PreparedStatement stmt = super.getConn().prepareStatement("Select * from People");

                ResultSet rset = stmt.executeQuery();
                ResultSetMetaData rsmd = rset.getMetaData();
                int numColumns = rsmd.getColumnCount();
                //System.out.println("numcolumns is " + numColumns);
                String columnHeader = "";
                String columnHeaderLine = "";
                for (int i = 1; i <= numColumns; i++) {
                    String colHeaderName = rsmd.getColumnName(i);
                    columnHeader += colHeaderName + "\t";
                    String hypens = colHeaderName.replaceAll("\\w", "-");
                    columnHeaderLine += hypens + "\t";
                }
                String rowString = columnHeader + "\n" + columnHeaderLine + "\n";
                while (rset.next()) {
                    for (int i = 1; i <= numColumns; i++) {
                        Object o = rset.getObject(i);
                        rowString += o.toString() + "\t";
                    }
                    rowString += "\n";
                }
                // System.out.print("rowString  is  " + rowString);
                textArea.setText(rowString);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                System.err.println("SQL Error:" + e.getMessage());
                e.printStackTrace();
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                System.err.println("Error in QueryButtonListener:" + e1.getMessage());
                textArea.append("Please select a database.\n");
            }
        }
    }

    private List<ComboBoxItem> getNames() throws SQLException {

        List<ComboBoxItem> comboList = new ArrayList<>();
        try {
            PreparedStatement stmt = clientDB.getConn().prepareStatement("Select * from People where sent=0");
            ResultSet rset = stmt.executeQuery();
            while (rset.next()) {
                String name = rset.getString("first") + " " + rset.getString("last");
                int id = rset.getInt("id");
                ComboBoxItem newItem = new ComboBoxItem(id, name);
                comboList.add(newItem);
            }
            // System.out.print("comboList  is  " + comboList);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            System.err.println("SQL Error in getNames:" + e.getMessage());
            e.printStackTrace();
        }
        return comboList;
    }

    // a JComboBox will take a bunch of objects and use the "toString()" method
    // of those objects to print out what's in there.
    // So I have provided to you an object to put people's names and ids in
    // and the combo box will print out their names.
    // now you will want to get the ComboBoxItem object that is selected in the combo box
    // and get the corresponding row in the People table and make a person object out of that.
    class ComboBoxItem {
        private int id;
        private String name;

        public ComboBoxItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public String toString() {
            return this.name;
        }
    }

    public static void main(String[] args) {
        ClientInterface ci = new ClientInterface();
        ci.setVisible(true);
    }
}
