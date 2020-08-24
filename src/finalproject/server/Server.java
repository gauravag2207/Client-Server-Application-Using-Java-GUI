/* Gaurav Agrawal (ga1380) Final Project */

package finalproject.server;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import finalproject.db.DBInterface;
import finalproject.entities.Person;

public class Server extends JFrame implements Runnable {

    public static final int DEFAULT_PORT = 8001;
    private static final int FRAME_WIDTH = 600;
    private static final int FRAME_HEIGHT = 800;
    final int AREA_ROWS = 10;
    final int AREA_COLUMNS = 40;

    private JPanel topPanel;
    private JPanel dbPanel;
    private JPanel dataBtnPanel;
    private JLabel dbName;
    private JTextArea textArea;
    private JMenuBar menuBar;
    private JButton queryDataBtn;

    DBInterface serverDB;

    // Number a client
    private int clientNo = 0;


    public Server() throws IOException, SQLException {
        this(DEFAULT_PORT, "server.db");
    }

    public Server(String dbFile) throws IOException, SQLException {
        this(DEFAULT_PORT, dbFile);
    }

    public Server(int port, String dbFile) throws IOException, SQLException {

        this.setSize(Server.FRAME_WIDTH, Server.FRAME_HEIGHT);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener((e) -> System.exit(0));
        menu.add(exitItem);
        menuBar.add(menu);
        this.setJMenuBar(menuBar);

        dbPanel = new JPanel();
        dbPanel.add(new JLabel("DB: "));
        dbName = new JLabel("<None>");
        dbPanel.add(dbName);

        dataBtnPanel = new JPanel();
        queryDataBtn = new JButton("Query DB");
        dataBtnPanel.add(queryDataBtn);

        textArea = new JTextArea(AREA_ROWS, AREA_COLUMNS); // creating a new textArea
        textArea.setEditable(false); // making the frame non editable

        JScrollPane textAreaScroller = new JScrollPane(textArea);

        topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.add(dbPanel);
        topPanel.add(dataBtnPanel);
        this.add(topPanel, BorderLayout.NORTH);
        this.add(textAreaScroller);
        try {
            // Connect to a database
            serverDB = new DBInterface();
            Connection conn = DriverManager.getConnection
                    ("jdbc:sqlite:server.DB");
            serverDB.setConnection(conn);
            dbName.setText("server.DB");
            queryDataBtn.addActionListener(new QueryButtonListener());
        } catch (Exception e) {
            System.exit(0);
        }
        Thread t = new Thread(this);
        t.start();
    }

    class QueryButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            try {
                PreparedStatement stmt = serverDB.getConn().prepareStatement("Select * from People");
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
                //System.out.print("rowString  is  " + rowString);
                textArea.append("DB Results:\n");
                textArea.append(rowString);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                System.err.println("SQL Error in QueryButtonListener:" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        Server sv;
        try {
            sv = new Server("server.db");
            sv.setVisible(true);
        } catch (IOException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        try {
            // Create a server socket
            ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);
            textArea.append("Listening on port 8001\n");
            while (true) {
                try {
                    // Listen for a connection request
                    Socket socket = serverSocket.accept();
                    clientNo++; // Increment clientNo
                    textArea.append("Starting thread for client " + clientNo +
                            " at " + new Date() + '\n');
                    // Find the client's host name, and IP address
                    InetAddress inetAddress = socket.getInetAddress();
                    textArea.append("Client " + clientNo + "'s host name is "
                            + inetAddress.getCanonicalHostName() + "\n");
                    textArea.append("Client " + clientNo + "'s IP Address is "
                            + inetAddress.getHostAddress() + "\n");

                    new Thread(new HandleAClient(socket, clientNo)).start();
                } catch (IOException ex) {
                    System.out.println("Error13");
                    break;
                    //ex.printStackTrace();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Define the thread class for handling new connection
    class HandleAClient implements Runnable {
        private Socket socket; // A connected socket
        private int clientNum;

        /**
         * Construct a thread
         */
        public HandleAClient(Socket socket, int clientNum) {
            this.socket = socket;
            this.clientNum = clientNum;
        }

        /**
         * Run a thread
         */
        public void run() {
            try {
                textArea.append("Listening for input from Client " + clientNo + "\n");
                // Create Object input and data output streams
                ObjectInputStream inputFromClient = new ObjectInputStream(
                        socket.getInputStream());
                DataOutputStream outputToClient = new DataOutputStream(
                        socket.getOutputStream());

                // Continuously serve the client
                while (true) {
                    // Read from input
                    try {
                        Object object = inputFromClient.readObject();

                        Person receivedPerson = (Person) object;

                        textArea.append("got Person " + receivedPerson.toString() + " inserting into DB\n");

                        PreparedStatement stmt = serverDB.getConn().prepareStatement("insert into People values(?,?,?,?,?,?)");
                        stmt.setString(1, receivedPerson.getFirst());
                        stmt.setString(2, receivedPerson.getLast());
                        stmt.setInt(3, receivedPerson.getAge());
                        stmt.setString(4, receivedPerson.getCity());
                        stmt.setInt(5, 1);
                        stmt.setInt(6, receivedPerson.getId());

                        int isSuccess = stmt.executeUpdate();
                        if (isSuccess > 0) {
                            textArea.append("Inserted Successfully\n");
                            outputToClient.writeBytes("Success\n");
                        } else {
                            outputToClient.writeBytes("Failed\n");
                        }
                    } catch (IOException ex) {
                        System.err.println("Error11 "+ ex.getMessage());
                        //ex.printStackTrace();
                        textArea.append("Connection Ended\n");
                        break;
                    } catch (ClassNotFoundException e) {
                        outputToClient.writeBytes("Failed\n");
                        break;
                    } catch (SQLException e) {
                        outputToClient.writeBytes("Failed\n");
                    }
                }
            } catch (IOException ex) {
                System.err.println("Error12 "+ex.getMessage());
                //ex.printStackTrace();
                textArea.append("Connection Ended\n");
            }
        }
    }
}
