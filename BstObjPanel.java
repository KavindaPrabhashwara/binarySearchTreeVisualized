import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class BstObjPanel extends JPanel implements Runnable {

    private final static double zoomMax = 5;
    private final static double zoomMin = 0.5;
    final public BstObjShape treeShape = new BstObjShape();
    final public Point2D.Double initialPoint = new Point2D.Double();
    private final int delay = 1000;
    private final ConcurrentLinkedDeque<Task> tasksToExecute = new ConcurrentLinkedDeque<Task>();
    private final LinkedList<Object> taskArguments = new LinkedList<Object>();
    private double x;
    private double y;
    private double mouseX;
    private double mouseY;
    private NodeShape mouseIsOverNode = null;
    /**
     * If true, the panel will not allow zooming. If false, use the scroll wheel
     * to zoom in and out.
     */
    private boolean zoomIsLocked = false;
    /**
     * If true, the panel will not allow panning. If false, the panel will pan
     * when the mouse clicks, and then drags.
     */
    private boolean screenIsLocked = false;
    /**
     * A zoom value of 1 means that the images are being displayed normally.
     */
    private double zoom = 1;
    /**
     * The last x value for the mouse before the affine transform was applied,
     * and the frame was updated
     */
    private double lastX;
    /**
     * The last y value for the mouse before the affine transform was applied,
     * and the frame was updated
     */
    private double lastY;

    private PersonGenerator personGenerator = new PersonGenerator();

    private int speed = 0;
    private volatile boolean running = true;
    private volatile boolean paused = false;

    public BstObjPanel() {
        setOpaque(false);
        setVisible(true);
        setFocusable(true);

        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL
                        && !zoomIsLocked) {
                    incrementZoom(.05 * -(double) e.getWheelRotation());
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!screenIsLocked) {
                    lastX = e.getX();
                    lastY = e.getY();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                x = e.getX();
                y = e.getY();
            }

            public void mouseDragged(MouseEvent e) {
                if (!screenIsLocked) {
                    final int newMouseX = e.getX();
                    final int newMouseY = e.getY();

                    Point2D adjPreviousPoint = getAffineTranslatedPoint(new Point2D.Double(
                            lastX, lastY));

                    lastX = newMouseX;
                    lastY = newMouseY;

                    Point2D adjNewPoint = getAffineTranslatedPoint(new Point2D.Double(
                            newMouseX, newMouseY));

                    double newX = adjNewPoint.getX() - adjPreviousPoint.getX();
                    double newY = adjNewPoint.getY() - adjPreviousPoint.getY();

                    mouseX += newX;
                    mouseY += newY;

                    repaint();
                }
            }
        });
    }

    public static void main(String[] args) throws Exception {
        final BstObjPanel bstObjPanel = new BstObjPanel();
//        bstObjPanel.treeShape.buildRandomTree(bstObjPanel.personGenerator, 1.0,
//                0.25, true);

        final Thread thread = new Thread(bstObjPanel);
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                final JFrame frame = new JFrame("Binary Search Tree Display") {
                    @Override
                    public void dispose() {
                        bstObjPanel.running = false;
                        super.dispose();
                    }
                };
                frame.setLayout(new GridBagLayout());
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JFrame.setDefaultLookAndFeelDecorated(true);

                // Create the weights for the main graphical panel
                GridBagConstraints c = new GridBagConstraints();
                c.fill = GridBagConstraints.BOTH;
                c.ipadx = 500;
                c.ipady = 500;
                c.gridwidth = 100;
                c.gridheight = 100;
                c.gridx = GridBagConstraints.RELATIVE;
                c.anchor = GridBagConstraints.LINE_START;
                c.gridy = 0;
                c.weighty = 1.0;
                c.weightx = 1.0;
                // add the panel with weights
                frame.add(bstObjPanel, c);

                // Create the weights for the main graphical panel
                c = new GridBagConstraints();
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 0; // 0.125
                c.weighty = 1.0;
                c.ipadx = 0;
                c.ipady = 50;
                c.gridwidth = 1;
                c.gridheight = 1;
                c.gridx = GridBagConstraints.RELATIVE;
                c.anchor = GridBagConstraints.LINE_START;

                // TODO make this print out data to the lower right corner
                final JLabel[] data = null;// = new JLabel[];
                final JPanel dataPanel = new JPanel() {
                    {
                        setLayout(new GridLayout(2, 2));
                        // add(data[0]); //Total number of nodes
                    }
                };

                final JPanel buttonPanel = new JPanel() {
                    {
                        setLayout(new GridLayout(0, 1));
                        add(new JButton("Lock Zoom") {
                            {
                                this.addActionListener(new ActionListener() {
                                    private String base = " Zoom";
                                    private String[] values = { "Lock",
                                            "Unlock" };
                                    private int v = 0;

                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        ((JButton) e.getSource())
                                                .setText(values[v = ((v + 1) % values.length)]
                                                        + base);
                                        bstObjPanel.toggleZoom();
                                    }
                                });
                            }
                        });
                        add(new JButton("Lock Panning") {
                            {
                                this.addActionListener(new ActionListener() {
                                    private String base = " Panning";
                                    private String[] values = { "Lock",
                                            "Unlock" };
                                    private int v = 0;

                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        ((JButton) e.getSource())
                                                .setText(values[v = ((v + 1) % values.length)]
                                                        + base);
                                        bstObjPanel.toggleScreenPanning();
                                    }
                                });
                            }
                        });
                        add(new TreeNodeFunctionButton(bstObjPanel, frame,
                                "Delete"));
                        add(new TreeNodeFunctionButton(bstObjPanel, frame,
                                "Find"));
                        add(new TreeNodeFunctionButton(bstObjPanel, frame,
                                "Insert"));
                        add(new JButton("Pre-Order") {
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        // bstObjPanel.paused = true;
                                    }
                                });
                            }
                        });
                        add(new JButton("In-Order") {
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        // bstObjPanel.paused = true;
                                    }
                                });
                            }
                        });
                        add(new JButton("Post-Order") {
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        // bstObjPanel.paused = true;
                                    }
                                });
                            }
                        });
                        add(new JButton("Is Empty") {
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        // bstObjPanel.paused = true;
                                    }
                                });
                            }
                        });
                        // TODO delete this button or replace it with something
                        // that is not immediate. Useful for debug though.
                        add(new JButton("Randomize Tree") {
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        bstObjPanel.paused = true;
                                        bstObjPanel.treeShape.buildRandomTree(
                                                bstObjPanel.personGenerator,
                                                1.0, 0.04, false);
                                        bstObjPanel.paused = false;
                                    }
                                });
                            }
                        });
                        add(new JButton("Clear Tree") {
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        bstObjPanel.treeShape.root = null;
                                    }
                                });
                            }
                        });
                        add(new JButton("Insert N random nodes"){
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        bstObjPanel.treeShape.root = null;
                                        int i = Integer.parseInt(JOptionPane.showInputDialog("Please type the number of nodes you want to randomely insert", "50"));
                                        while(i > 0) {
                                            bstObjPanel.addTaskToFront(new Task("insert",bstObjPanel.personGenerator.generateRandomPerson(), bstObjPanel.treeShape.root));
                                            i--;
                                        }
                                    }
                                });
                            }
                        });
                        add(new JButton("consectetur"));
                        add(new JButton("Lorem"));
                        add(new JButton("ipsum"));
                        add(new JButton("dolor"));
                        add(new JButton("Speed Up") {
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        if (bstObjPanel.speed < 10 - 1) {
                                            bstObjPanel.speed++;
                                        }
                                    }
                                });
                            }
                        });
                        add(new JButton("Slow Down") {
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        if (bstObjPanel.speed > -10 + 1) {
                                            bstObjPanel.speed--;
                                        }
                                    }
                                });
                            }
                        });
                        add(new JButton("Toggle Pause") {
                            {
                                this.addActionListener(new ActionListener() {

                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        bstObjPanel.paused = !bstObjPanel.paused;
                                    }
                                });
                            }
                        });
                        add(new JButton("Reset Speed") {
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        bstObjPanel.speed = 0;
                                    }
                                });
                            }
                        });
                        add(new JButton("Close") {
                            {
                                this.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        frame.dispose();
                                    }
                                });
                            }
                        });
                    }
                };
                JPanel sidePanel = new JPanel() {
                    {
                        setLayout(new GridLayout(2, 2));
                        add(buttonPanel);
                        add(dataPanel);
                    }
                };
                frame.add(sidePanel, c);
                frame.setUndecorated(true);

                Dimension screenSize = Toolkit.getDefaultToolkit()
                        .getScreenSize();
                frame.setSize(screenSize.width, screenSize.height);
                frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
        thread.run();
    }

    public void run() {
        final long startTime = System.currentTimeMillis();
        while (running) {

            System.out.println((System.currentTimeMillis() - startTime) + "");
            if(!tasksToExecute.isEmpty()) {
                tasksToExecute.poll().exectute(this);
            }
            
            repaint();
            try {
                int i = 0;
                while (true) {
                    for (; i < delay - (delay * speed) / 10; i += 10) {
                        if (paused) {
                            break;
                        }
                        Thread.sleep(10);
                        updateMouse();
                    }
                    if (paused) {
                        do {
                            Thread.sleep(100);
                        } while (paused);
                        continue;
                    }
                    break;
                }

            } catch (InterruptedException e) {
            }
        }

    }

    private void updateMouse() {
        boolean repaint = true;
        if (treeShape.root != null) {
            NodeShape current = null;
            if (mouseIsOverNode != null) {
                current = mouseIsOverNode;
            }
            mouseIsOverNode = treeShape.root
                    .contains(normalize(new Point2D.Double(x, y)));
            
            if(current != null && current.equals(mouseIsOverNode)) {
                repaint = true;
            } else {
                if(current!= null) {
                    current.unselect();
                    repaint = true;
                }
                if(mouseIsOverNode != null) {
                    mouseIsOverNode.select();
                    repaint = true;
                }
            }
        }
        else {
            if (mouseIsOverNode != null) {
                mouseIsOverNode.unselect();
                repaint = true;
            }
            mouseIsOverNode = null;
        }
        if (repaint) {
            this.repaint();
        }
    }

    public void toggleZoom() {
        zoomIsLocked = !zoomIsLocked;
    }

    public void toggleScreenPanning() {
        screenIsLocked = !screenIsLocked;
    }

    public void incrementZoom(double amount) {
        if (amount < 0) {
            zoom -= Math.min(.5, (Math.pow(2,
                    (zoom + Math.abs(amount) - (zoomMax + zoomMin) / 2))) / 25);
        }
        else {
            zoom += Math
                    .min(.5,
                            (Math.pow(
                                    2,
                                    -(zoom + Math.abs(amount) - (zoomMax + zoomMin) / 2))) / 25);
        }
        zoom = Math.min(zoomMax, Math.max(zoomMin, zoom));
        repaint();
    }

    public void paintComponent(Graphics g) {
        Graphics2D gd = (Graphics2D) g.create();

        // Set constants for rendering
        gd.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        gd.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gd.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);

        // Draw anything you do not want to be affected by transformations, aka,
        // anything you would like to be drawn over the Binary Search Tree.
        // Useful for debugging

        // Apply transform for data
        gd.setTransform(getAffineTransform());

        // Apply translation for data
        gd.translate(30, -getHeight() / 2 + 50);
        // Draw the Binary Search tree
        treeShape.draw(gd);

        // Dispose of the graphics
        gd.dispose();
    }

    private AffineTransform getAffineTransform() {
        AffineTransform tx = new AffineTransform();

        tx.translate(getWidth() / 2, getHeight() / 2);
        tx.scale(zoom, zoom);
        tx.translate(mouseX, mouseY);

        return tx;
    }

    private Point2D normalize(Point2D point) {
        return getStaticlyTranslatedPoint(getAffineTranslatedPoint(point));
    }

    private Point2D getStaticlyTranslatedPoint(Point2D point) {
        return new Point2D.Double(point.getX() - 30, point.getY()
                + (getHeight() / 2 + 50) - 98);
    }

    private Point2D getAffineTranslatedPoint(Point2D point) {
        try {
            return getAffineTransform().inverseTransform(point, null);
        } catch (NoninvertibleTransformException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void pause() {
        this.paused = true;
    }

    public void unpause() {
        this.paused = false;
    }

    public synchronized void addTaskToFront(Task newTask) {
        tasksToExecute.addFirst(newTask);
    }

    public synchronized void addTaskToEnd(Task newTask) {
        tasksToExecute.addLast(newTask);
    }

    public int taskArgumentSize() {
        return taskArguments.size();
    }

    /**
     * Get the next argument
     * 
     * @return
     */
    public Object getNextTaskArgument() {
        return taskArguments.poll();
    }

    /**
     * Added in order to the end of the queue
     */
    public void addTaskArgument(Object... c) {
        Collections.addAll(taskArguments, c);
    }

    public int getTaskArgumentsSize() {
        return taskArguments.size();
    }
}