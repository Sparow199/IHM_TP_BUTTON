import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import fr.lri.swingstates.canvas.CExtensionalTag;
import fr.lri.swingstates.canvas.CRectangle;
import fr.lri.swingstates.canvas.CShape;
import fr.lri.swingstates.canvas.CStateMachine;
import fr.lri.swingstates.canvas.CText;
import fr.lri.swingstates.canvas.Canvas;
import fr.lri.swingstates.canvas.transitions.EnterOnShape;
import fr.lri.swingstates.canvas.transitions.LeaveOnShape;
import fr.lri.swingstates.canvas.transitions.PressOnShape;
import fr.lri.swingstates.canvas.transitions.ReleaseOnShape;
import fr.lri.swingstates.debug.StateMachineEvent;
import fr.lri.swingstates.debug.StateMachineEventAdapter;
import fr.lri.swingstates.debug.StateMachineVisualization;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.transitions.Release;
import fr.lri.swingstates.sm.transitions.TimeOut;

public class SimpleButton extends MouseAdapter {

    private static final String RIGHT_CLICK = "Right click";
    private static final String ERROR_RIGHT_CLICK_DOESNT_WORK = "Right click doesn't work ";
    private static final String LEFT_CLICK = "Left click";
    private static final String BUTTON_NAME = "simple";
    private static CText label;
    private static CRectangle rectangle;

    SimpleButton(final Canvas canvas, final String text) {
        label = canvas.newText(0, 0, text, new Font("verdana", Font.PLAIN, 12));

        double x = label.getMinX();
        double y = label.getMinY();
        double w = label.getHeight();
        double h = label.getWidth();
        rectangle = canvas.newRectangle(x - 5, y - 5, h + 10, w + 10);
        rectangle.setFillPaint(Color.white);
        label.above(rectangle);
        label.addChild(rectangle);
        rectangle.getFillPaint();
        canvas.addMouseListener(this);
    }

    public void mouseClicked(final MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            System.err.println(LEFT_CLICK);
        } else {
            System.err.println(ERROR_RIGHT_CLICK_DOESNT_WORK);
        }
    }

    /**
     * add/remove yellow color
     */
    final static CExtensionalTag yellow = new CExtensionalTag() {
        public void added(CShape s) {
            s.setOutlined(true).setFillPaint(Color.yellow);
        };

        public void removed(CShape s) {
            s.setOutlined(true).setFillPaint(Color.white);

        };
    };

    /**
     * add/remove red color on double click
     */
    final static CExtensionalTag red = new CExtensionalTag() {
        public void added(CShape s) {
            s.setOutlined(true).setFillPaint(Color.red);
        };

        public void removed(CShape s) {
            s.setOutlined(true).setFillPaint(Color.white);
        };
    };

    /**
     * method that gather transactions
     */
    public static void statesFactory() {
        JFrame frame = new JFrame();
        final Canvas canvas = new Canvas(400, 400);
        frame.getContentPane().add(canvas);
        frame.pack();
        frame.setVisible(true);

        final SimpleButton myButton = new SimpleButton(canvas, BUTTON_NAME);
        SimpleButton.getShape().translateBy(100, 100);

		/*
		 * state machine creation
		 */
        CStateMachine sm = new CStateMachine() {

            /*
             * initial state
             */
            public State idle = new State() {
                final Transition enterOnShape = new EnterOnShape(">> on") {
                    public void action() {
                        rectangle.setStroke(new BasicStroke(2));
                    }
                };
            };
            /*
             * state on hover
             */
            public State on = new State() {
                final Transition leaveOnShape = new LeaveOnShape(">> idle") {
                    public void action() {
                        rectangle.setStroke(new BasicStroke(1));
                    }
                };
                final Transition pressOnShape = new PressOnShape(BUTTON1, ">> semiClick") {
                    public void action() {
                        rectangle.addTag(yellow);
                        rectangle.setStroke(new BasicStroke(1));
                        /* timer used to back to 'on' state */
                        armTimer(1000, false);
                    }
                };

            };

            /**
             * state on press button
             */
            public State press = new State() {
                /* timer to back to 'on' state */
                final Transition timeOut = new TimeOut(">> on") {
                    public void action() {
                        rectangle.removeTag(yellow);
                        rectangle.setStroke(new BasicStroke(2));
                    }
                };
                /* switch to state 'clickAndHalf' if we have second click */
                final Transition pressOnShape = new PressOnShape(BUTTON1, ">> clickAndHalf") {
                    public void action() {
                        rectangle.addTag(red);
                        rectangle.setStroke(new BasicStroke(1));
                        /* timer to back to state 'on' */
                        armTimer(1000, false);// Timer qui permettra de revenir
                    }
                };
            };

            /**
             * state on press out of button
             */
            public State outButton = new State() {
                /* return in button area, state back to 'semiClick' and restart timer */
                final Transition enterOnShape = new EnterOnShape(">> semiClick") {
                    public void action() {
                        rectangle.addTag(yellow);
                        rectangle.setStroke(new BasicStroke(1));
                        armTimer(1000, false);
                    }
                };
                /* release click, back to state 'idle' */
                final Release release = new Release(">> idle") {
                    public void action() {
                        rectangle.removeTag(yellow);
                        rectangle.setStroke(new BasicStroke(1));
                    }
                };

            };

            /**
             * state semiClick
             */
            public State semiClick = new State() {
                /* release click, return to state 'press' */
                final Transition releaseOnShape = new ReleaseOnShape(">> press") {
                    public void action() {
                        rectangle.addTag(yellow);
                        rectangle.setStroke(new BasicStroke(1));
                        /* Timer faster */
                        armTimer(250, false);
                    }
                };
                /* press out of button and stay clicked, back to state 'outButton' */
                final Transition leaveOnShape = new LeaveOnShape(">> outButton") {
                    public void action() {
                        // disarmTimer();
                        rectangle.removeTag(yellow);
                        rectangle.setStroke(new BasicStroke(1));
                    }
                };
                /* timeout reached, back to state 'on' */
                final Transition timeOut = new TimeOut(">> on") {
                    public void action() {
                        rectangle.removeTag(yellow);
                        rectangle.setStroke(new BasicStroke(2));
                    }
                };

            };

            /**
             * state double click
             */
            public State doubleClick = new State() {
                /* timer to back to 'on' state */
                final Transition timeOut = new TimeOut(">> on") {
                    public void action() {
                        rectangle.removeTag(red);
                        rectangle.setStroke(new BasicStroke(2));
                    }
                };
            };

            /**
             * state click and half
             */
            public State clickAndHalf = new State() {
                /* timer to back to 'on' state */
                final Transition timeOut = new TimeOut(">> on") {
                    public void action() {
                        rectangle.removeTag(red);
                        rectangle.setStroke(new BasicStroke(2));
                    }
                };
                /* release click, return to state 'doubleClick' */
                final Transition releaseOnShape = new ReleaseOnShape(">> doubleClick") {
                    public void action() {
                        rectangle.addTag(red);
                        rectangle.setStroke(new BasicStroke(1));
                        armTimer(250, false);
                    }
                };

            };

        };
        sm.armTimer(450, true);

        /* show state machine */
        final JFrame viz = new JFrame();
        viz.getContentPane().add(new StateMachineVisualization(sm));
        viz.pack();
        viz.setVisible(true);

        /* show state machine on transitions */
        sm.attachTo(canvas);
        sm.addStateMachineListener(new StateMachineEventAdapter() {
            public void smStateChanged(StateMachineEvent e) {
                System.out.println("State changed from " + e.getPreviousState().getName() + " to "
                        + e.getCurrentState().getName() + "\n");
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    
    /**
     * @return CShape the label
     */
    public static CShape getShape() {
        return label;
    }

    /**
     * show mouse events
     */
    public void mousePressed(final MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            System.out.println(LEFT_CLICK);
        } else {
            System.out.println(RIGHT_CLICK);
        }
    }


    public static void main(String[] args) {

        SimpleButton.statesFactory();
    }


}