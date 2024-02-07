/*
  Icon Editor #1 - Simple Icon Editor for *.ICO Files in Windows
  Written by: Keith Fenske, http://kwfenske.github.io/
  Thursday, 22 March 2007
  Java class name: IconEdit1
  Copyright (c) 2007 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 graphical (GUI) application to edit icon files (*.ICO) for
  Windows.  An icon file has several images in sizes like 16x16, 24x24, 32x32,
  48x48, and 64x64 pixels.  Icons are square from 8x8 to 256x256 pixels.
  Colors may be 4-bit (16 colors), 8-bit (256 colors), or 24-bit (millions).
  For 4-bit color, only the standard Windows color palette is used, although
  other palettes will be read and converted.  For 8-bit color, only the
  standard 216 "web safe" colors are used.  Other palettes will be read and
  converted.  There are no restrictions on 24-bit colors; all RGB (red-green-
  blue) values are accepted.  Pixels may be transparent and let the background
  show through.  Please note that IconEdit is an old program and does not
  support alpha channels, compressed data (PNG images), or icons larger than
  256 pixels.

  When you first run IconEdit, you are given a selection of icon sizes and a
  partially-hidden dialog for choosing colors.  You may read icons from a file
  with the "Open Icon File" menu item.  Icons will appear in the same order as
  they are defined in the file.  Tabs on the left-hand side identify icons by
  their size; click on a tab to select an icon.  To change an icon, first
  select a color with the color chooser.  Then left click (primary mouse click)
  on an icon square to paint the selected color.  Right click (or control
  click) to erase a square and make it transparent, which is shown as the
  current background color (see the slider).  Shift click on a square to select
  its color without painting.  You may save all non-empty icons to a new file
  with the "Save Icon File" menu item.

  These drawing tools are crude.  This program is meant more for loading icons,
  making minor changes, and saving them again.  For better drawing tools, use
  your favorite bitmapped image editor and copy-and-paste to this application.
  Images on the system clipboard don't retain transparency data.  This was
  added to the BufferedImage class starting in Java 5.0 (1.5).

  Linux and Macintosh users should note that "favicon.ico" files used to
  bookmark web pages can be created with this program, because they are in fact
  Windows icons.

  Apache License or GNU General Public License
  --------------------------------------------
  IconEdit1 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options for the position and size of the
  application window, and the size of the display font.  Only one additional
  parameter may be given: the name of an icon file to open for reading.  See
  the "-?" option for a help summary:

      java  IconEdit1  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.

  Restrictions and Limitations
  ----------------------------
  Icons are actually more complicated than having solid colors mixed with
  transparent squares.  Standard icons in Windows use two bit masks, first to
  AND existing screen pixels, and second to XOR icon pixels onto the screen.
  For the most part, this amounts to replacing or not replacing screen pixels
  with icon pixels.  However, an icon can be designed to invert screen pixels
  by setting bits in both the AND mask and the XOR mask.  Such an icon doesn't
  contain its own picture information, isn't recognized by this program, and
  may open as ugly color data.  Another new type of icon in Windows Vista has
  24-bit color (8-bit red, 8-bit green, 8-bit blue) with an 8-bit alpha channel
  to control the degree of transparency.  IconEdit ignores alpha channels and
  uses the AND mask for fully transparent or fully opaque (solid).

  Suggestions for New Features
  ----------------------------
  (1) Allow the icon height and width to be different.  This is interesting
      only from a programming point of view.  Nobody would use such a feature.
      Being asked for a height and a width instead of just a single size might
      become a real nuisance.  KF, 2007-04-01.
  (2) Should have an "undo" feature, and inexperienced users like confirmation
      for destructive changes ("clear" buttons, an offer to save file on exit,
      etc).  KF, 2007-11-07.
  (3) Alpha channels, compressed data, and really big icons are not supported.
      You need more advanced software for that.  KF, 2014-12-28.
  (4) The 216 "web safe" colors are used for 256-color icons, so eight colors
      are missing that are in the Windows 16-color palette.  KF, 2014-12-28.
  (5) Allow custom colors for 4-bit and 8-bit palettes.  This could be loaded
      from an option on the command line.  KF, 2021-11-05.
  (6) Drawing icons would be faster if we didn't try to find the closest color
      in the current palette.  Faster but not as much fun.  KF, 2021-11-05.
*/

import java.awt.*;                // older Java GUI support
import java.awt.datatransfer.*;   // clipboard
import java.awt.event.*;          // older Java GUI event support
import java.awt.image.*;          // images
import java.io.*;                 // standard I/O
import java.text.*;               // date and number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support
import javax.swing.border.*;      // decorative borders
import javax.swing.event.*;       // color selection dialog

public class IconEdit1
{
  /* constants */

  static final Color BACKGROUND = new Color(204, 204, 204);
                                  // use only shades of gray, because they
                                  // don't affect our recognition of colors
  static final int BIT_MASK = 0x00000001; // gets low-order bit from integer
  static final int BYTE_MASK = 0x000000FF; // gets low-order byte from integer
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2007 by Keith Fenske.  Apache License or GNU GPL.";
  static final int[][] DEFAULT_COLOR_16 = {{0, 0, 0}, {128, 0, 0}, {0, 128, 0},
    {128, 128, 0}, {0, 0, 128}, {128, 0, 128}, {0, 128, 128}, {192, 192, 192},
    {128, 128, 128}, {255, 0, 0}, {0, 255, 0}, {255, 255, 0}, {0, 0, 255},
    {255, 0, 255}, {0, 255, 255}, {255, 255, 255}}; // do *not* change order!
                                  // the Windows 16-color palette, RGB values
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final String EMPTY_STATUS =
    "Click to paint.  Right (control) click to erase.  Shift click to select.";
                                  // message when no status to display
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
  static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final Color GRID_COLOR = new Color(153, 153, 153); // to draw gridlines
  static final int MAX_SIZE = 256; // maximum supported icon height or width;
                                  // set MAX_SIZE to 255 for strict standards,
                                  // or 256 for icons that may or may not work
  static final int MIN_HEIGHT = 500; // minimum window height in pixels
  static final int MIN_WIDTH = 700; // minimum window width in pixels
  static final int NIBBLE_MASK = 0x0000000F; // low-order four bits from integer
  static final String PROGRAM_TITLE =
    "Simple Icon Editor for *.ICO Files in Windows - by: Keith Fenske";
  static final String SYSTEM_FONT = "Dialog"; // this font is always available

  /* class variables */

  static boolean allowEmptyIcons; // true if we save empty icons (no data)
  static Font buttonFont;         // font for buttons, labels, status, etc
  static JColorChooser colorChooser; // standard Java color selection tool
  static JDialog colorDialog;     // non-modal dialog box for <colorChooser>
  static int[][] colorPalette16;  // 16-color (4-bit) RGB palette for all icons
  static JButton colorSample;     // our display of currently selected color
  static JFileChooser fileChooser; // asks for input and output file names
  static String hiddenText;       // hidden text string when saving icon files
  static javax.swing.filechooser.FileFilter iconFilter;
                                  // our shared file filter for icons
  static JFrame mainFrame;        // this application's window for GUI
  static JMenuBar menuBar;        // always visible menu bar
  static JMenuItem menuClearAll, menuDelete, menuEraseColor, menuExit,
    menuFileOpen, menuFileSave, menuGridBack, menuGridHide, menuGridNorm,
    menuIconClear, menuIconCopy, menuIconPaste, menuMoveDown, menuMoveUp,
    menuNew16, menuNew24, menuNew32, menuNew48, menuNew64, menuNew96,
    menuNew128, menuNew192, menuNew256, menuNewSize, menuShowColors, menuText;
                                  // various menu items
  static JMenu menuFile, menuIcon, menuNew, menuView; // main (visible) menus
  static ButtonGroup menuGridGroup; // group of radio buttons for grid choices
  static JMenu menuGridSub;       // sub-menu for selecting gridline choices
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static Color selectColor;       // user's selected color from <colorChooser>
  static JLabel statusDialog;     // status message during extended processing
  static JTabbedPane tabbedPane;  // tabbed pane with one panel per icon image
  static int userIconSize;        // last icon size given by user as a number

/*
  main() method

  We run as a graphical application only.  Set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Border emptyBorder;           // remove borders around text areas
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    String userFileName;          // name of one icon file
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables. */

    allowEmptyIcons = false;      // don't save empty icons (no color data)
    buttonFont = null;            // by default, don't use customized font
    colorPalette16 = DEFAULT_COLOR_16; // Windows 16-color (4-bit) RGB palette
    mainFrame = null;             // during setup, there is no GUI window
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    userFileName = null;          // there is no default icon file name
    userIconSize = 32;            // default icon size when given as a number
    windowHeight = MIN_HEIGHT;    // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = MIN_WIDTH;

    /* Create a default hidden string that will be appended to the end of each
    new icon file.  The user can change this string with the "Set Hidden Text"
    menu item.  If the string is empty, then no text will be written. */

    hiddenText =
      "This icon file was created by the IconEdit1 Java application on "
      + (new SimpleDateFormat("yyyy-MM-dd")).format(new Date()) + ".";

    /* Check command-line parameters for options. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      else if (word.equals("-e") || (mswinFlag && word.equals("/e")))
        allowEmptyIcons = true;   // yes, save empty icons with no color data

      else if (word.startsWith("-p") || (mswinFlag && word.startsWith("/p")))
      {
        /* This undocumented option reads a 16-color (4-bit) palette from a
        file.  The same palette is used for all 16-color icons, which is better
        than having no choice, but is not the same as customizable palettes for
        each icon.  There is presently no feature for changing the 256-color
        (8-bit) palette, as that is strongly tied to "web safe" colors. */

        loadPalette16(args[i].substring(2)); // accept anything for file name
      }

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        buttons, dialogs, labels, etc. */

        int size = -1;            // default value for font point size
        try                       // try to parse remainder as unsigned integer
        {
          size = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          size = -1;              // set result to an illegal value
        }
        if ((size < 10) || (size > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        buttonFont = new Font(SYSTEM_FONT, Font.PLAIN, size); // for big sizes
//      buttonFont = new Font(SYSTEM_FONT, Font.BOLD, size); // for small sizes
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // yes, maximize our main window

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }

      else
      {
        /* Parameter does not look like an option.  Accept one file name. */

        if (userFileName == null) // if we don't already have a file name
          userFileName = args[i]; // then this becomes the one and only
        else
        {
          System.err.println("Too many file names on command line: "
            + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }
    }

    /* Open the graphical user interface (GUI).  The standard Java style is the
    most reliable, but you can switch to something closer to the local system,
    if you want. */

//  try
//  {
//    UIManager.setLookAndFeel(
//      UIManager.getCrossPlatformLookAndFeelClassName());
////    UIManager.getSystemLookAndFeelClassName());
//  }
//  catch (Exception ulafe)
//  {
//    System.err.println("Unsupported Java look-and-feel: " + ulafe);
//  }

    /* Initialize shared graphical objects. */

    action = new IconEdit1User(); // create our shared action listener
    emptyBorder = BorderFactory.createEmptyBorder(); // for removing borders
    fileChooser = new JFileChooser(); // create our shared file chooser
    iconFilter = new IconEdit1Filter(); // create our shared file filter

    fileChooser.setFileFilter(iconFilter); // show only certain types of files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file

    /* Create the graphical interface as a series of little panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel1, panel2, etc). */

    /* Create a horizontal panel for the menu bar.  Once upon a time, there
    were action buttons here, but the menu bar does a better job, is easier to
    set up and use, and handles keyboard shortcuts. */

    menuBar = new JMenuBar();
    menuBar.setBackground(BACKGROUND);
    menuBar.setBorder(emptyBorder); // no border necessary here
    if (buttonFont != null) menuBar.setFont(buttonFont); // has no effect
//  menuBar.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

    /* This is the pop-up "File" menu, affects all icons, all sizes. */

    menuFile = new JMenu("File");
    if (buttonFont != null) menuFile.setFont(buttonFont);
    menuFile.setMnemonic(KeyEvent.VK_F);
//  menuFile.setToolTipText("Open and save icon files.");

    menuClearAll = new JMenuItem("Clear All Icons");
    menuClearAll.addActionListener(action);
    if (buttonFont != null) menuClearAll.setFont(buttonFont);
//  menuClearAll.setMnemonic(KeyEvent.VK_C);
    menuFile.add(menuClearAll);

    menuFileOpen = new JMenuItem("Open Icon File...");
    menuFileOpen.addActionListener(action);
    menuFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
      InputEvent.CTRL_MASK));
    if (buttonFont != null) menuFileOpen.setFont(buttonFont);
    menuFileOpen.setMnemonic(KeyEvent.VK_O);
    menuFile.add(menuFileOpen);

    menuFileSave = new JMenuItem("Save Icon File...");
    menuFileSave.addActionListener(action);
    menuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
      InputEvent.CTRL_MASK));
    if (buttonFont != null) menuFileSave.setFont(buttonFont);
    menuFileSave.setMnemonic(KeyEvent.VK_S);
    menuFile.add(menuFileSave);

    menuFile.addSeparator();

    menuExit = new JMenuItem("Exit (Close)");
    menuExit.addActionListener(action);
    if (buttonFont != null) menuExit.setFont(buttonFont);
//  menuExit.setMnemonic(KeyEvent.VK_X);
    menuFile.add(menuExit);

    menuBar.add(Box.createHorizontalGlue()); // put 1/3 extra space here
    menuBar.add(menuFile);
    menuBar.add(Box.createHorizontalStrut(30));

    /* This is the pop-up "View" menu, for general appearance. */

    menuView = new JMenu("View");
    if (buttonFont != null) menuView.setFont(buttonFont);
    menuView.setMnemonic(KeyEvent.VK_V);
//  menuView.setToolTipText("Colors and appearance.");

    menuMoveDown = new JMenuItem("Move Tab Down");
    menuMoveDown.addActionListener(action);
    menuMoveDown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
      InputEvent.CTRL_MASK));
    if (buttonFont != null) menuMoveDown.setFont(buttonFont);
    menuMoveDown.setMnemonic(KeyEvent.VK_D);
    menuMoveDown.setDisplayedMnemonicIndex(9);
    menuView.add(menuMoveDown);

    menuMoveUp = new JMenuItem("Move Tab Up");
    menuMoveUp.addActionListener(action);
    menuMoveUp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
      InputEvent.CTRL_MASK));
    if (buttonFont != null) menuMoveUp.setFont(buttonFont);
    menuMoveUp.setMnemonic(KeyEvent.VK_U);
    menuView.add(menuMoveUp);

    menuText = new JMenuItem("Set Hidden Text...");
    menuText.addActionListener(action);
    if (buttonFont != null) menuText.setFont(buttonFont);
    menuText.setMnemonic(KeyEvent.VK_H);
    menuView.add(menuText);

    menuShowColors = new JMenuItem("Show Colors");
    menuShowColors.addActionListener(action);
    if (buttonFont != null) menuShowColors.setFont(buttonFont);
    menuShowColors.setMnemonic(KeyEvent.VK_C);
    menuView.add(menuShowColors);

    menuGridGroup = new ButtonGroup(); // begin sub-menu for gridline choices
    menuGridSub = new JMenu("Show Gridlines");
    if (buttonFont != null) menuGridSub.setFont(buttonFont);
    menuGridSub.setMnemonic(KeyEvent.VK_G);

    menuGridBack = new JRadioButtonMenuItem("Background Color", false);
    menuGridBack.addActionListener(action);
    if (buttonFont != null) menuGridBack.setFont(buttonFont);
    menuGridBack.setMnemonic(KeyEvent.VK_B);
    menuGridGroup.add(menuGridBack);
    menuGridSub.add(menuGridBack);

    menuGridHide = new JRadioButtonMenuItem("Hide Gridlines", false);
    menuGridHide.addActionListener(action);
    if (buttonFont != null) menuGridHide.setFont(buttonFont);
    menuGridHide.setMnemonic(KeyEvent.VK_H);
    menuGridGroup.add(menuGridHide);
    menuGridSub.add(menuGridHide);

    menuGridNorm = new JRadioButtonMenuItem("Normal (Mid Gray)", true);
    menuGridNorm.addActionListener(action);
    if (buttonFont != null) menuGridNorm.setFont(buttonFont);
    menuGridNorm.setMnemonic(KeyEvent.VK_N);
    menuGridGroup.add(menuGridNorm);
    menuGridSub.add(menuGridNorm);

    menuView.add(menuGridSub);    // end of sub-menu for gridline choices
    menuBar.add(menuView);
    menuBar.add(Box.createHorizontalStrut(30));

    /* This is the pop-up "New" menu, to add new icon sizes. */

    menuNew = new JMenu("New");
    if (buttonFont != null) menuNew.setFont(buttonFont);
    menuNew.setMnemonic(KeyEvent.VK_N);
//  menuNew.setToolTipText("Add new icon sizes.");

    menuNew16 = new JMenuItem("New 16 x 16");
    menuNew16.addActionListener(action);
    if (buttonFont != null) menuNew16.setFont(buttonFont);
    menuNew16.setMnemonic(KeyEvent.VK_1);
    menuNew.add(menuNew16);

    menuNew24 = new JMenuItem("New 24 x 24");
    menuNew24.addActionListener(action);
    if (buttonFont != null) menuNew24.setFont(buttonFont);
    menuNew24.setMnemonic(KeyEvent.VK_2);
    menuNew.add(menuNew24);

    menuNew32 = new JMenuItem("New 32 x 32");
    menuNew32.addActionListener(action);
    if (buttonFont != null) menuNew32.setFont(buttonFont);
    menuNew32.setMnemonic(KeyEvent.VK_3);
    menuNew.add(menuNew32);

    menuNew48 = new JMenuItem("New 48 x 48");
    menuNew48.addActionListener(action);
    if (buttonFont != null) menuNew48.setFont(buttonFont);
    menuNew48.setMnemonic(KeyEvent.VK_4);
    menuNew.add(menuNew48);

    menuNew64 = new JMenuItem("New 64 x 64");
    menuNew64.addActionListener(action);
    if (buttonFont != null) menuNew64.setFont(buttonFont);
    menuNew64.setMnemonic(KeyEvent.VK_6);
    menuNew.add(menuNew64);

    menuNew96 = new JMenuItem("New 96 x 96");
    menuNew96.addActionListener(action);
    if (buttonFont != null) menuNew96.setFont(buttonFont);
    menuNew96.setMnemonic(KeyEvent.VK_9);
    menuNew.add(menuNew96);

    menuNew128 = new JMenuItem("New 128 x 128");
    menuNew128.addActionListener(action);
    if (buttonFont != null) menuNew128.setFont(buttonFont);
    menuNew.add(menuNew128);

    menuNew192 = new JMenuItem("New 192 x 192");
    menuNew192.addActionListener(action);
    if (buttonFont != null) menuNew192.setFont(buttonFont);
    menuNew.add(menuNew192);

    menuNew256 = new JMenuItem("New 256 x 256");
    menuNew256.addActionListener(action);
    if (buttonFont != null) menuNew256.setFont(buttonFont);
    if (MAX_SIZE >= 256) menuNew.add(menuNew256);

    menuNewSize = new JMenuItem("Choose Size...");
    menuNewSize.addActionListener(action);
    if (buttonFont != null) menuNewSize.setFont(buttonFont);
    menuNewSize.setMnemonic(KeyEvent.VK_S);
    menuNewSize.setDisplayedMnemonicIndex(7);
    menuNew.add(menuNewSize);

    menuBar.add(menuNew);
    menuBar.add(Box.createHorizontalStrut(30));

    /* This is the pop-up "Icon" menu, specific to one icon, one size. */

    menuIcon = new JMenu("Icon");
    if (buttonFont != null) menuIcon.setFont(buttonFont);
    menuIcon.setMnemonic(KeyEvent.VK_I);
//  menuIcon.setToolTipText("Clear, copy, paste.");

    menuIconClear = new JMenuItem("Clear This Icon");
    menuIconClear.addActionListener(action);
    if (buttonFont != null) menuIconClear.setFont(buttonFont);
    menuIcon.add(menuIconClear);

    menuIconCopy = new JMenuItem("Copy This Icon");
    menuIconCopy.addActionListener(action);
    menuIconCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
      InputEvent.CTRL_MASK));
    if (buttonFont != null) menuIconCopy.setFont(buttonFont);
    menuIconCopy.setMnemonic(KeyEvent.VK_C);
    menuIcon.add(menuIconCopy);

    menuIconPaste = new JMenuItem("Paste This Icon");
    menuIconPaste.addActionListener(action);
    menuIconPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
      InputEvent.CTRL_MASK));
    if (buttonFont != null) menuIconPaste.setFont(buttonFont);
    menuIconPaste.setMnemonic(KeyEvent.VK_P);
    menuIcon.add(menuIconPaste);

    menuIcon.addSeparator();

    menuDelete = new JMenuItem("Delete This Icon");
    menuDelete.addActionListener(action);
    if (buttonFont != null) menuDelete.setFont(buttonFont);
    menuIcon.add(menuDelete);

    menuEraseColor = new JMenuItem("Erase Selected Color");
    menuEraseColor.addActionListener(action);
    menuEraseColor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
      InputEvent.CTRL_MASK));
    if (buttonFont != null) menuEraseColor.setFont(buttonFont);
    menuEraseColor.setMnemonic(KeyEvent.VK_E);
    menuIcon.add(menuEraseColor);

    menuBar.add(menuIcon);
    menuBar.add(Box.createHorizontalGlue()); // put 2/3 extra space here
    menuBar.add(Box.createHorizontalGlue());
    JPanel panel1 = new JPanel(new BorderLayout(0, 0));
    panel1.setBackground(BACKGROUND);
    panel1.add(menuBar, BorderLayout.CENTER);
    panel1.add(Box.createVerticalStrut(2), BorderLayout.SOUTH);

    /* Create a tabbed pane for drawing each size of icon.  Like the menu bar
    above, and the status message below, a border layout allows for margins to
    be added later, if necessary. */

    tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
    tabbedPane.setBackground(BACKGROUND);
    if (buttonFont != null) tabbedPane.setFont(buttonFont);
    clearFileIcons();             // create a selection of empty icons

    JPanel panel3 = new JPanel(new BorderLayout(0, 0));
    panel3.setBackground(BACKGROUND);
    panel3.add(tabbedPane, BorderLayout.CENTER);

    /* Standard Java color selection tool.  This tool doesn't resize, and its
    shape doesn't fit in our layout, so use the tool as a dialog box. */

    colorChooser = new JColorChooser();
    colorChooser.getSelectionModel().addChangeListener((ChangeListener) action);
    colorDialog = JColorChooser.createDialog(mainFrame, "Select Color", false,
      colorChooser, null, null);
    colorDialog.setLocation((windowLeft + 150), (windowTop + 150));
//  colorDialog.setVisible(true); // show the color selection tool

    /* Create a panel for the status message and the button that displays or
    redisplays the color selection tool.  We do this to have control over the
    margins.  Put the status text in the middle of a border layout so that it
    expands with the window size. */

    JPanel panel4 = new JPanel(new BorderLayout(10, 0));
    panel4.setBackground(BACKGROUND);
    statusDialog = new JLabel(COPYRIGHT_NOTICE, JLabel.LEFT);
    if (buttonFont != null) statusDialog.setFont(buttonFont);
    panel4.add(statusDialog, BorderLayout.CENTER);

    colorSample = new JButton("RGB (255,255,255)"); // button used as label
    colorSample.addActionListener(action);
    colorSample.setFocusable(false); // skip this button when using Tab key
    if (buttonFont != null) colorSample.setFont(buttonFont);
    colorSample.setToolTipText("Selected color for painting.");
    getSelectedColor();           // and ask color chooser for current color
    panel4.add(colorSample, BorderLayout.EAST);

    JPanel panel5 = new JPanel(new BorderLayout(0, 0));
    panel5.setBackground(BACKGROUND);
    panel5.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
    panel5.add(Box.createHorizontalStrut(3), BorderLayout.WEST);
    panel5.add(panel4, BorderLayout.CENTER);

    /* Stack buttons and options on top of a border layout.  Keep tabbed pane
    in the center so that it expands horizontally and vertically.  Put status
    message at the bottom (expands horizontally). */

    JPanel panel6 = new JPanel(new BorderLayout(0, 0));
    panel6.setBackground(BACKGROUND);
    panel6.add(panel1, BorderLayout.NORTH);
    panel6.add(panel3, BorderLayout.CENTER);
    panel6.add(panel5, BorderLayout.SOUTH);

    /* Create the main window frame for this application. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    JPanel panel7 = (JPanel) mainFrame.getContentPane();
    panel7.setBackground(BACKGROUND);
    panel7.setLayout(new BorderLayout(0, 0));
    panel7.add(Box.createVerticalStrut(11), BorderLayout.NORTH);
    panel7.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panel7.add(panel6, BorderLayout.CENTER);
    panel7.add(Box.createHorizontalStrut(13), BorderLayout.EAST);
    panel7.add(Box.createVerticalStrut(9), BorderLayout.SOUTH);

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    mainFrame.setSize(Math.max(windowWidth, MIN_WIDTH),
      Math.max(windowHeight, MIN_HEIGHT)); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
//  mainFrame.setVisible(true);   // and then show application window

    /* If there was an argument (parameter) on the command line, attempt to
    open that as an icon file.  Otherwise, give the user a selection of empty
    icons in various common sizes.  Default icons are created above with the
    JTabbedPane object to avoid a race condition in JRE 1.4 to 7 and possibly
    later, and will be recreated as empty if the file read fails.  This race
    condition may cause an array index error in BasicTabbedPaneUI.paintTabArea
    and can prevent our menu buttons from appearing! */

    if (userFileName != null)     // if a parameter was given on command line
      readIconFile(new File(userFileName)); // then open it as an icon file
//  else
//    clearFileIcons();           // create a selection of empty icons

    /* Let the graphical interface run the application now. */

    mainFrame.setVisible(true);   // and then show application window

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  canonFileName() method

  In most of this program, we don't care what a user types for a file name, so
  long as it gets accepted by the system.  In the title bar, we much prefer to
  have the exact or "official" file name, for example, lowercase and uppercase
  on Windows.
*/
  static String canonFileName(File givenFile)
  {
    String result;                // our best effort at exact file name

    try { result = givenFile.getCanonicalFile().getName(); } // canonical
    catch (IOException ioe) { result = givenFile.getName(); }  // abstract
    return(result);               // give caller whatever we could find
  }


/*
  canWriteFile() method

  The caller gives us a Java File object.  We return true if it seems safe to
  write to this file.  That is, if the file doesn't exist and we can create a
  new file, or if the file exists and the user gives permission to replace it.
*/
  static boolean canWriteFile(File givenFile)
  {
    boolean result;               // status flag that we return to the caller

    if (givenFile.isDirectory())  // can't write to folders/directories
    {
      JOptionPane.showMessageDialog(mainFrame, (givenFile.getName()
        + " is a directory or folder.\nPlease select a normal file."));
      result = false;             // don't try to open this "file" for writing
    }
    else if (givenFile.isHidden()) // won't write to hidden (protected) files
    {
      JOptionPane.showMessageDialog(mainFrame, (givenFile.getName()
        + " is a hidden or protected file.\nPlease select a normal file."));
      result = false;
    }
    else if (givenFile.isFile() == false) // are we creating a new file?
    {
      result = true;              // assume we can create new file by this name
    }
    else if (givenFile.canWrite()) // file exists, but can we write to it?
    {
      result = (JOptionPane.showConfirmDialog(mainFrame, (givenFile.getName()
        + " already exists.\nDo you want to replace this with a new file?"))
        == JOptionPane.YES_OPTION);
    }
    else                          // if we can't write to an existing file
    {
      JOptionPane.showMessageDialog(mainFrame, (givenFile.getName()
        + " is locked or write protected.\nCan't write to this file."));
      result = false;
    }
    return(result);               // give caller our best guess about writing

  } // end of canWriteFile() method


/*
  clearFileIcons() method

  Remove any and all existing icon tabs from the tabbed pane.  Then create new
  tabs in a selection of icon sizes.
*/
  static void clearFileIcons()
  {
    tabbedPane.removeAll();       // remove, delete, deallocate existing panels
    tabbedPane.addTab("16 x 16", new IconEdit1Icon(16)); // empty 16x16 icon
    tabbedPane.addTab("24 x 24", new IconEdit1Icon(24)); // empty 24x24 icon
    tabbedPane.addTab("32 x 32", new IconEdit1Icon(32)); // empty 32x32 icon
    tabbedPane.addTab("48 x 48", new IconEdit1Icon(48)); // empty 48x48 icon
    tabbedPane.addTab("64 x 64", new IconEdit1Icon(64)); // empty 64x64 icon
//  tabbedPane.addTab("96 x 96", new IconEdit1Icon(96)); // empty 96x96 icon
//  tabbedPane.addTab("128 x 128", new IconEdit1Icon(128)); // empty 128x128 icon
//  tabbedPane.addTab("192 x 192", new IconEdit1Icon(192)); // empty 192x192 icon
//  tabbedPane.addTab("256 x 256", new IconEdit1Icon(256)); // empty 256x256 icon
  }


/*
  colorsAreWebSafe() method

  Return true if a Color array contains only RGB colors from the "web safe"
  palette of 216 colors (usually stored as 8-bit indices).
*/
  static boolean colorsAreWebSafe(Color[][] array)
  {
    int blue, green, red;         // individual RGB components of a color
    int column, row;              // zero-based indices into color data
    boolean result;               // our result that we return to the caller

    result = true;                // assume that all colors are valid
    for (row = 0; row < array.length; row ++)
    {
      for (column = 0; column < array[row].length; column ++)
      {
        blue = array[row][column].getBlue(); // get blue component
        green = array[row][column].getGreen();
        red = array[row][column].getRed();
        if (((blue % 51) != 0) || ((green % 51) != 0) || ((red % 51) != 0))
        {
          result = false;         // this color is not web safe
          break;                  // exit early from "column" for loop
        }
      }
      if (!result)                // have we already found an exception?
        break;                    // then exit early from "row" for loop
    }
    return(result);               // tell caller what we found

  } // end of colorsAreWebSafe() method


/*
  colorsAreWindows16() method

  Return true if a Color array contains only RGB colors from the Windows 16-
  color palette (usually stored as 4-bit indices).
*/
  static boolean colorsAreWindows16(Color[][] array)
  {
    int blue, green, red;         // individual RGB components of a color
    int column, row;              // zero-based indices into color data
    boolean found;                // true if we match one specific color
    boolean result;               // our result that we return to the caller
    int sixteen;                  // index into pre-defined 16-color RGB values

    result = true;                // assume that all colors are valid
    for (row = 0; row < array.length; row ++)
    {
      for (column = 0; column < array[row].length; column ++)
      {
        blue = array[row][column].getBlue(); // get blue component
        green = array[row][column].getGreen();
        red = array[row][column].getRed();
        found = false;            // haven't matched this color entry yet
        for (sixteen = 0; sixteen < colorPalette16.length; sixteen ++)
        {
          if ((red == colorPalette16[sixteen][0])
            && (green == colorPalette16[sixteen][1])
            && (blue == colorPalette16[sixteen][2]))
          {
            found = true;         // this array entry matches pre-defined color
            break;                // exit early from "sixteen" for loop
          }
        }
        if (!found)               // did we fail to match this array entry?
        {
          result = false;         // yes, failed: don't look at more entries
          break;                  // exit early from "column" loop
        }
      }
      if (!result)                // have we already found an exception?
        break;                    // then exit early from "row" for loop
    }
    return(result);               // tell caller what we found

  } // end of colorsAreWindows16() method


/*
  createNewIconSize() method

  Create a tabbed panel for a new icon with a size chosen by the user, within
  the limits of what we can handle.  This code is slightly more complicated
  than what should normally go in the UserButton() action handler.  Note that
  large sizes may create a noticeable delay during drawing on older computers.
*/
  static void createNewIconSize()
  {
    String answer;                // input received from user, or null
    int number;                   // parsed input as an integer

    answer = JOptionPane.showInputDialog(mainFrame,
      ("Enter an icon size from 8 to " + MAX_SIZE + " pixels:"),
      String.valueOf(userIconSize));
    if (answer != null)           // null result means user cancelled dialog
    {
      try { number = Integer.parseInt(answer); } // try to parse as integer
      catch (NumberFormatException nfe) { number = -1; } // set to invalid
      if ((number < 8) || (number > MAX_SIZE))
        JOptionPane.showMessageDialog(mainFrame,
          ("Please enter an integer from 8 to " + MAX_SIZE
          + " for the icon size,\n"
          + "without extra spaces or punctuation (decimal point, etc)."));
      else
      {
        tabbedPane.addTab((number + " x " + number),
          new IconEdit1Icon(number)); // create tabbed panel for new icon size
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        userIconSize = number;    // remember same number for next time
      }
    }
  } // end of createNewIconSize() method


/*
  eraseSelectedColor()

  Make transparent any pixels in the current icon that have the same color as
  the selected color.  This is useful when pasting images from other sources,
  because the system clipboard doesn't retain transparency data.  If the source
  marks the background with a distinct color (which is the usual procedure),
  then it is easy to remove all pixels with that particular color.
*/
  static void eraseSelectedColor()
  {
    int column, row;              // zero-based indices into color data
    int eraseCount;               // number of pixels that we erased/replaced
    IconEdit1Icon thisIcon;       // our current icon object with real data
    int thisIndex;                // index of currently selected icon tab

    thisIndex = tabbedPane.getSelectedIndex(); // get index of selected icon
    if (thisIndex < 0)
      statusDialog.setText("Can't erase colors if no icon tab is selected.");
    else
    {
      eraseCount = 0;             // no pixels/squares erased/replaced yet
      thisIcon = (IconEdit1Icon) tabbedPane.getComponentAt(thisIndex);
      for (row = 0; row < thisIcon.size; row ++)
      {
        for (column = 0; column < thisIcon.size; column ++)
        {
          if (thisIcon.colors[row][column].equals(selectColor))
          {                       // exact comparison, no fuzzy tolerance
            if (thisIcon.isOpaque[row][column]) // don't count background erase
              eraseCount ++;      // increment number of pixels erased/replaced
            thisIcon.colors[row][column] = Color.BLACK; // fade to black (joke)
            thisIcon.isOpaque[row][column] = false; // color is now transparent
          }
        }
      }
      thisIcon.repaint();         // repaint icon panel after changes if any

      if (eraseCount > 1)         // provide user with some feedback
        statusDialog.setText("Found and erased/replaced " + eraseCount
          + " pixels/squares.");
      else if (eraseCount > 0)    // if exactly one pixel erased/replaced
        statusDialog.setText(
          "Found and erased/replaced only one pixel/square.");
      else
        statusDialog.setText("Sorry, no pixels/squares have the RGB color ("
          + selectColor.getRed() + "," + selectColor.getGreen() + ","
          + selectColor.getBlue() + ").");
    }
  } // end of eraseSelectedColor() method


/*
  getCloseColor() method

  Given a color, return the closest standard color in a selected color depth.
*/
  static Color getCloseColor(int colorDepth, Color givenColor)
  {
    Color result;                 // the color that we return to the caller

    if (colorDepth == 4)          // Windows 4-bit color (16 colors)
    {
      int index = getCloseWin16Index(givenColor);
                                  // get index of closest in 16-color palette
      result = new Color(colorPalette16[index][0], colorPalette16[index][1],
        colorPalette16[index][2]);
    }
    else if (colorDepth == 8)     // web-safe 8-bit color (256 colors)
    {
      result = new Color(
        (((int) ((givenColor.getRed() + 25.5) / 51.0)) * 51),
        (((int) ((givenColor.getGreen() + 25.5) / 51.0)) * 51),
        (((int) ((givenColor.getBlue() + 25.5) / 51.0)) * 51));
    }
    else                          // assume 24-bit color (millions)
      result = givenColor;

    return(result);               // give caller the color that best fits

  } // end of getCloseColor() method


/*
  getCloseWin16Index() method

  Get the index of the closest color in the standard 16-color Windows palette.
  We use a "least squares" approximation on the RGB components.
*/
  static int getCloseWin16Index(Color givenColor)
  {
    int bestBlue, bestGreen, bestRed; // calculated color components
    int bestDelta, thisDelta;     // difference compared to standard colors
    int givenBlue, givenGreen, givenRed; // original color components
    int result;                   // color index that we return to the caller
    int thisBlue, thisGreen, thisRed; // calculated color components

    bestBlue = bestGreen = bestRed = 0; // just to keep compiler happy
    bestDelta = 999999999;        // makes anything else look good
    givenBlue = givenColor.getBlue(); // get given red-green-blue components
    givenGreen = givenColor.getGreen();
    givenRed = givenColor.getRed();
    result = -1;                  // a deliberately illegal index value

    for (int i = 0; i < colorPalette16.length; i ++)
    {
      thisBlue = colorPalette16[i][2];
      thisGreen = colorPalette16[i][1];
      thisRed = colorPalette16[i][0];
      thisDelta = ((givenBlue - thisBlue) * (givenBlue - thisBlue))
        + ((givenGreen - thisGreen) * (givenGreen - thisGreen))
        + ((givenRed - thisRed) * (givenRed - thisRed));
      if (thisDelta < bestDelta)
      {
        bestBlue = thisBlue;      // remember RGB values of best color
        bestDelta = thisDelta;
        bestGreen = thisGreen;
        bestRed = thisRed;
        result = i;               // remember index of best color
      }
    }
    return(result);               // give caller our best guess about color

  } // end of getCloseWin16Index() method


/*
  getSelectedColor() method

  Get the currently selected color from the color chooser and update variables
  and dialogs to match.
*/
  static void getSelectedColor()
  {
    int blue, green, red;         // individual RGB components of this color

    selectColor = colorChooser.getColor(); // get composite color
    blue = selectColor.getBlue(); // get blue component
    green = selectColor.getGreen();
    red = selectColor.getRed();

    /* Paint our informational label with the selected color and using a
    contrasting color for the text. */

    colorSample.setBackground(selectColor); // redraw label with new color
//  colorSample.setForeground(new Color(((red + 128) % 256),
//    ((green + 128) % 256), ((blue + 128) % 256)));
    colorSample.setForeground(new Color(((red < 128) ? 255 : 0),
      ((green < 128) ? 255 : 0), ((blue < 128) ? 255 : 0)));
    colorSample.setText("RGB (" + red + "," + green + "," + blue +")");

  } // end of getSelectedColor() method


/*
  loadPalette16() method

  Read a 16-color (4-bit) RGB palette from a text file.  The same palette will
  be used for all 16-color icons.  If there is an error, print a warning and
  continue with the default palette.

  Input lines may have an RGB value followed by a comment.  Both are optional.
  Hence, an input line can be empty or just white space (blanks or tabs).  RGB
  values are three unsigned decimal numbers separated by white space.  Comments
  start with a "#" character.
*/
  static void loadPalette16(String fileName)
  {
    int blue, green, red;         // color values from 0 to 255
    int i, k;                     // index variables
    BufferedReader input;         // input stream for reading from file
    String line;                  // one complete input line from file
    Matcher matcher;              // apply regular expression to one line
    final int maxSize = 16;       // give a name to the number of colors
    final int minSize = 2;        // minimum number of colors we accept
    Pattern pattern;              // same regular expression for all lines
    int[][] result;               // where we build up a new palette
    int used;                     // number of palette entries complete

    /* Create an empty palette where all colors are black. */

    result = new int[maxSize][3]; // one red, green, blue for each entry
    for (i = 0; i < maxSize; i ++)
      for (k = 0; k < 3; k ++)
        result[i][k] = 0;
    used = 0;                     // none of the array has been used yet

    /* Create a regular expression to scan and validate each input line. */

    pattern = Pattern.compile(
      "\\s*(?:(\\d{1,5})\\s+(\\d{1,5})\\s+(\\d{1,5}))?\\s*(?:#.*)?");

    /* Read lines from the input file. */

    try                           // allow for I/O errors, missing files, etc
    {
      input = new BufferedReader(new FileReader(fileName)); // open input file
      while ((line = input.readLine()) != null) // read one input line
      {
        matcher = pattern.matcher(line); // attempt to match entire line
        if (matcher.matches())    // if the input line has proper syntax
        {
          if ((matcher.group(1) != null) && (matcher.group(1).length() > 0))
          {
            /* There is an RGB value: numbers for red, green, and blue. */

            if (used >= maxSize)  // is the palette already full?
            {
              System.err.println("Palette file has more than " + maxSize
                + " entries: " + line);
              result = null;      // cancel current palette
              break;              // stop reading
            }
            red = Integer.parseInt(matcher.group(1)); // parse integers
            green = Integer.parseInt(matcher.group(2));
            blue = Integer.parseInt(matcher.group(3));
            if ((blue > 255) || (green > 255) || (red > 255)) // unsigned
            {
              System.err.println("Palette file out of range (0-255): " + line);
              result = null;      // cancel current palette
              break;              // stop reading
            }
            result[used][0] = red; // save color values
            result[used][1] = green;
            result[used][2] = blue;
            used ++;              // one more entry done
          }
        }
        else                      // even simple syntax can go bad
        {
          System.err.println("Palette file syntax error: " + line);
          result = null;          // cancel current palette
          break;                  // stop reading
        }
      }
      input.close();              // close input file
    }
    catch (IOException ioe)
    {
      System.err.println("Palette file I/O error: " + ioe.getMessage());
      result = null;              // cancel current palette
    }

    /* While a palette could be empty, this makes no practical sense. */

    if ((result != null) && (used < minSize))
    {
      System.err.println("Palette file needs at least " + minSize
        + " entries: " + fileName);
      result = null;              // cancel current palette
    }

    /* If we are happy with the new palette, assign it as our global variable.
    Otherwise, revert back to the default palette. */

    colorPalette16 = (result == null) ? DEFAULT_COLOR_16 : result;

  } // end of loadPalette16() method


/*
  readIconFile() method

  Read a single *.ICO file and copy each icon to a new tab in the tabbed pane.
  The tabs will appear in the same order as the icons in the file.
*/
  static void readIconFile(File givenFile)
  {
    int column, row;              // zero-based indices into color data
    long fileSize;                // length of file in bytes
    File inputFile;               // user's selected input file
    IconEdit1Icon ourIcon;        // our icon object, filled with our data
    Color[] palette;              // color palette for current icon
    RandomAccessFile ramFile;     // file stream for reading icon file
    int padBytes;                 // number of zero bytes at end of each row
    boolean stopFlag;             // local flag to stop processing file
    String stopText;              // message for why we stopped processing

    /* Ask the user for an input file name, if we weren't already given a file
    by our caller. */

    if (givenFile == null)        // should we ask the user for a file name?
    {
      fileChooser.setDialogTitle("Open Icon File...");
      if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
        return;                   // user cancelled file selection dialog box
      inputFile = fileChooser.getSelectedFile(); // get user's input file
    }
    else                          // caller gave us file name (may not exist)
    {
      fileChooser.setCurrentDirectory(givenFile); // remember this directory
      fileChooser.setSelectedFile(givenFile); // remember caller's file name
      inputFile = givenFile;      // use caller's file name without asking
    }

    /* Initialize local variables. */

    fileSize = inputFile.length(); // get length of file in bytes
    stopFlag = false;             // everything is okay so far
    stopText = "";                // no reason for stopping yet
    tabbedPane.removeAll();       // remove, delete, deallocate existing panels

    /* Read bytes from the input file.  The format of an icon file is too
    detailed and too obscure to include a written description here.  See the
    Microsoft Windows documentation, or search for the following keywords on
    the internet: BITMAPINFOHEADER, ICONDIR, ICONDIRENTRY, ICONIMAGE.  As much
    as possible, we use the official Microsoft names for all data fields. */

    try                           // to catch file I/O errors
    {
      ramFile = new RandomAccessFile(inputFile, "r");

      /* The next "for" loop gives us a nearly clean way to stop processing
      early without messy "go to" statements. */

      for (int dummy = 0; dummy < 1; dummy ++)
      {
        if (fileSize < 6)         // minimum size for file header
        {
          stopFlag = true;        // stop looking at this file
          stopText = "File too small for icon header: fileSize " + fileSize
            + ".";
          break;                  // exit early from "dummy" for loop
        }

        /* Read the icon directory (ICONDIR), that is, the file header. */

        int idReserved = readIconUnsignedShort(ramFile);
        int idType = readIconUnsignedShort(ramFile);
        int idCount = readIconUnsignedShort(ramFile);

        if ((idReserved != 0) || ((idType != 1) && (idType != 2))
          || (idCount < 1) || (idCount > 99))
        {
          stopFlag = true;          // stop looking at this file
          stopText = "Unsupported icon header: idReserved " + idReserved
            + ", idType " + idType + ", idCount " + idCount + ".";
          break;                  // exit early from "dummy" for loop
        }

        if (fileSize < ((idCount * 16) + 6)) // enough room for all entries?
        {
          stopFlag = true;        // stop looking at this file
          stopText = "File too small for icon directory: fileSize " + fileSize
            + ", idCount " + idCount + ".";
          break;                  // exit early from "dummy" for loop
        }

        for (int iconIndex = 0; iconIndex < idCount; iconIndex ++)
        {
          /* Read the icon directory entry (ICONDIRENTRY). */

          ramFile.seek((iconIndex * 16) + 6);
          int bWidth = readIconUnsignedByte(ramFile);
          int bHeight = readIconUnsignedByte(ramFile);
          int bColorCount = readIconUnsignedByte(ramFile);
          int bReserved = readIconUnsignedByte(ramFile);
          int wPlanes = readIconUnsignedShort(ramFile);
          int wBitCount = readIconUnsignedShort(ramFile);
          int dwBytesInRes = readIconUnsignedInt(ramFile);
          long dwImageOffset = readIconUnsignedInt(ramFile);

          /* Values of zero (0x00) for the height or width are interpreted as
          256 (0x100).  This may have been poorly documented for years, until
          the days of Windows Vista (2007) and later.  Who are we to judge? */

          if (bHeight == 0) bHeight = 256;
          if (bWidth == 0) bWidth = 256;

          /* While icons can have many sizes, we only accept square icons where
          the height and width are the same. */

          if ((bHeight != bWidth) || (bHeight < 8) || (bHeight > MAX_SIZE))
          {
            stopFlag = true;        // stop looking at this file
            stopText = "Unsupported icon size: index " + iconIndex
              + ", bHeight " + bHeight + ", bWidth " + bWidth + ".";
            break;                  // exit early from "iconIndex" for loop
          }

          /* wBitCount often seems wrong or zero for many icons.  Ignore it
          when reading (but set it correctly when writing). */

          /* Do a rough check if all the data bytes fit within the file. */

          if (fileSize < (dwBytesInRes + dwImageOffset))
          {
            stopFlag = true;        // stop looking at this file
            stopText = "File too small for icon data: index " + iconIndex
              + ", dwImageOffset 0x" + Long.toHexString(dwImageOffset)
              + ", dwBytesInRes " + dwBytesInRes + ".";
            break;                  // exit early from "iconIndex" for loop
          }

          /* Read the bitmap information header (BITMAPINFOHEADER).  Much of
          this is unused or ignored on input, and set to reasonable values on
          output. */

          ramFile.seek(dwImageOffset);
          int biSize = readIconUnsignedInt(ramFile);
          int biWidth = readIconUnsignedInt(ramFile);
          int biHeight = readIconUnsignedInt(ramFile);
          int biPlanes = readIconUnsignedShort(ramFile);
          int biBitCount = readIconUnsignedShort(ramFile);
          int biCompression = readIconUnsignedInt(ramFile);
          int biSizeImage = readIconUnsignedInt(ramFile);
          int biXPelsPerMeter = readIconUnsignedInt(ramFile);
          int biYPelsPerMeter = readIconUnsignedInt(ramFile);
          int biClrUsed = readIconUnsignedInt(ramFile);
          int biClrImportant = readIconUnsignedInt(ramFile);

          /* Do some error checking on the bitmap header. */

          if ((biSize != 40) || (biWidth != bWidth)
            || (biHeight != (2 * bHeight)) || (biPlanes != 1))
          {
            stopFlag = true;        // stop looking at this file
            stopText = "Icon entry doesn't match header: index " + iconIndex
              + ", biWidth " + biWidth + ", biHeight " + biHeight + ".";
            break;                  // exit early from "iconIndex" for loop
          }

          /* The next steps involve reading actual picture data.  Create an
          icon object (of our own design) for storing this data. */

          ourIcon = new IconEdit1Icon(bWidth); // create empty, transparent

          /* Not all color depths are supported.  Small color depths always
          have a color palette, even if that palette is for the standard
          Windows colors. */

          if ((biBitCount == 1) && ((bColorCount == 0) || (bColorCount == 2)))
          {
            /* Icon is monochrome 1-bit color with two colors maximum.  There
            is always a color palette. */

            palette = new Color[2]; // allocate color palette
            for (int i = 0; i < 2; i ++) // read two colors
            {
              int rgbBlue = readIconUnsignedByte(ramFile);
              int rgbGreen = readIconUnsignedByte(ramFile);
              int rgbRed = readIconUnsignedByte(ramFile);
              int rgbReserved = readIconUnsignedByte(ramFile);
              palette[i] = new Color(rgbRed, rgbGreen, rgbBlue);
            }

            /* Read 1-bit XOR color data.  The AND mask will be read later.
            Rows must be padded to 32 bits (4 bytes) with extra zero bits or
            bytes. */

            padBytes = (((bWidth + 31) / 32) * 4) - ((bWidth + 7) / 8);
                                  // null bytes added to end of each row
            for (row = (bWidth - 1); row >= 0; row --) // bottom-to-top
            {
              int bitShift = -1;  // negative bit shift will force read byte
              int byteValue = 0;  // just to keep compiler happy
              for (column = 0; column < bWidth; column ++)
              {
                if (bitShift < 0) // have we already finished one byte?
                {
                  bitShift = 7;   // reset to high-order bit in byte
                  byteValue = readIconUnsignedByte(ramFile); // read 8 data bits
                }

                ourIcon.colors[row][column] = palette[(byteValue >> bitShift)
                  & BIT_MASK];    // get indexed color from palette
                ourIcon.isOpaque[row][column] = true; // unknown for now

                bitShift --;      // next column comes one bit lower
              }
              for (int i = 0; i < padBytes; i ++) // rows are padded to 32 bits
                readIconUnsignedByte(ramFile); // throw away extra bytes
            }
          }

          else if ((biBitCount == 4) && ((bColorCount == 0)
            || (bColorCount == 16)))
          {
            /* Icon is 4-bit color with 16 colors maximum.  There is always a
            color palette. */

            palette = new Color[16]; // allocate color palette
            for (int i = 0; i < 16; i ++) // read sixteen colors
            {
              int rgbBlue = readIconUnsignedByte(ramFile);
              int rgbGreen = readIconUnsignedByte(ramFile);
              int rgbRed = readIconUnsignedByte(ramFile);
              int rgbReserved = readIconUnsignedByte(ramFile);
              palette[i] = new Color(rgbRed, rgbGreen, rgbBlue);
            }

            /* Read 4-bit XOR color data.  The AND mask will be read later.
            Rows must be padded to 32 bits (4 bytes) with extra zero bits or
            bytes. */

            padBytes = (((bWidth + 7) / 8) * 4) - ((bWidth + 1) / 2);
                                  // null bytes added to end of each row
            for (row = (bWidth - 1); row >= 0; row --) // bottom-to-top
            {
              int bitShift = -1;  // negative bit shift will force read byte
              int byteValue = 0;  // just to keep compiler happy
              for (column = 0; column < bWidth; column ++)
              {
                if (bitShift < 0) // have we already finished one byte?
                {
                  bitShift = 4;   // reset to high-order nibble in byte
                  byteValue = readIconUnsignedByte(ramFile); // read 8 data bits
                }

                ourIcon.colors[row][column] = palette[(byteValue >> bitShift)
                  & NIBBLE_MASK]; // get indexed color from palette
                ourIcon.isOpaque[row][column] = true; // unknown for now

                bitShift -= 4;    // next column comes four bits lower
              }
              for (int i = 0; i < padBytes; i ++) // rows are padded to 32 bits
                readIconUnsignedByte(ramFile); // throw away extra bytes
            }
          }

          else if ((biBitCount == 8) && (bColorCount == 0))
          {
            /* Icon is 8-bit color with 256 colors maximum.  There is always a
            color palette.  The <bColorCount> field must be zero, and the
            <biClrUsed> and <biClrImportant> fields are not reliable. */

            palette = new Color[256]; // allocate color palette
            for (int i = 0; i < 256; i ++) // read 256 colors
            {
              int rgbBlue = readIconUnsignedByte(ramFile);
              int rgbGreen = readIconUnsignedByte(ramFile);
              int rgbRed = readIconUnsignedByte(ramFile);
              int rgbReserved = readIconUnsignedByte(ramFile);
              palette[i] = new Color(rgbRed, rgbGreen, rgbBlue);
            }

            /* Read 8-bit XOR color data.  The AND mask will be read later.
            Rows must be padded to 32 bits (4 bytes) with extra zero bytes. */

            padBytes = (((bWidth + 3) / 4) * 4) - bWidth;
                                  // null bytes added to end of each row
            for (row = (bWidth - 1); row >= 0; row --) // bottom-to-top
            {
              for (column = 0; column < bWidth; column ++)
              {
                int rgbIndex = readIconUnsignedByte(ramFile);
                ourIcon.colors[row][column] = palette[rgbIndex];
                ourIcon.isOpaque[row][column] = true; // until we read AND data
              }
              for (int i = 0; i < padBytes; i ++) // rows are padded to 32 bits
                readIconUnsignedByte(ramFile); // throw away extra bytes
            }
          }

          else if ((biBitCount == 24) && (bColorCount == 0))
          {
            /* Icon is 24-bit RGB (red-green-blue) with millions of colors.
            There is never a palette for RGB color.  RGB 24-bit has three bytes
            per pixel, which is different than RGBA 32-bit or palettes. */

            padBytes = ((((bWidth * 3) + 3) / 4) * 4) - (bWidth * 3);
                                  // null bytes added to end of each row
            for (row = (bWidth - 1); row >= 0; row --) // bottom-to-top
            {
              for (column = 0; column < bWidth; column ++)
              {
                int rgbBlue = readIconUnsignedByte(ramFile);
                int rgbGreen = readIconUnsignedByte(ramFile);
                int rgbRed = readIconUnsignedByte(ramFile);
// deleted //   int rgbAlpha = readIconUnsignedByte(ramFile); // only 3 bytes
                ourIcon.colors[row][column] = new Color(rgbRed, rgbGreen,
                  rgbBlue);
                ourIcon.isOpaque[row][column] = true; // until we read AND data
              }
              for (int i = 0; i < padBytes; i ++) // rows are padded to 32 bits
                readIconUnsignedByte(ramFile); // throw away extra bytes
            }
          }

          else if ((biBitCount == 32) && (bColorCount == 0))
          {
            /* Icon is 32-bit RGBA (red-green-blue-alpha) with millions of
            colors.  There is never a palette for RGB color.  We ignore the
            alpha channel, since the rest of this program can't handle it
            anyway.  We don't need to consider padding at the end of rows for
            32-bit color. */

            for (row = (bWidth - 1); row >= 0; row --) // bottom-to-top
              for (column = 0; column < bWidth; column ++)
              {
                int rgbBlue = readIconUnsignedByte(ramFile);
                int rgbGreen = readIconUnsignedByte(ramFile);
                int rgbRed = readIconUnsignedByte(ramFile);
                int rgbAlpha = readIconUnsignedByte(ramFile); // ignore
                ourIcon.colors[row][column] = new Color(rgbRed, rgbGreen,
                  rgbBlue);
                ourIcon.isOpaque[row][column] = true; // until we read AND data
              }
          }

          else
          {
            stopFlag = true;        // stop looking at this file
            stopText = "Unsupported color depth: index " + iconIndex
              + ", biBitCount " + biBitCount + ", bColorCount " + bColorCount
              + ".";
            break;                  // exit early from "iconIndex" for loop
          }

          /* Read the 1-bit AND mask for transparency information.  Rows must
          be padded to 32 bits (4 bytes) with extra zero bits or bytes. */

          padBytes = (((bWidth + 31) / 32) * 4) - ((bWidth + 7) / 8);
                                  // null bytes added to end of each row
          for (row = (bWidth - 1); row >= 0; row --) // bottom-to-top
          {
            int bitShift = -1;    // negative bit shift will force read byte
            int byteValue = 0;    // just to keep compiler happy
            for (column = 0; column < bWidth; column ++)
            {
              if (bitShift < 0)   // have we already finished one byte?
              {
                bitShift = 7;     // reset to high-order bit in byte
                byteValue = readIconUnsignedByte(ramFile); // read 8 data bits
              }

              if (((byteValue >> bitShift) & BIT_MASK) == 1)
              {
                ourIcon.colors[row][column] = Color.BLACK; // the "zero" value
                ourIcon.isOpaque[row][column] = false; // make transparent
              }

              bitShift --;        // next column goes one bit lower
            }
            for (int i = 0; i < padBytes; i ++) // rows are padded to 32 bits
              readIconUnsignedByte(ramFile); // throw away extra bytes
          }

          /* Estimate the color depth from RGB values used by the pixels. */

          if (colorsAreWindows16(ourIcon.colors)) // 16-color standard?
          {
            ourIcon.colorDepth = 4; // assume 4-bit color depth
            ourIcon.depthBit4.setSelected(true); // update radio button
          }
          else if (colorsAreWebSafe(ourIcon.colors)) // 256-color standard?
          {
            ourIcon.colorDepth = 8; // assume 8-bit color depth
            ourIcon.depthBit8.setSelected(true); // update radio button
          }
          else                    // colors are not from a known palette
          {
            ourIcon.colorDepth = 24; // assume 24-bit color depth
            ourIcon.depthBit24.setSelected(true); // update radio button
          }

          /* Create a tab in our GUI for this icon. */

          tabbedPane.addTab((bWidth + " x " + bWidth), ourIcon);

        } // end of "iconIndex" for loop

      } // end of "dummy" for loop

      /* We are done reading from the file.  Close the input file. */

      ramFile.close();            // try to close input file
      mainFrame.setTitle("Icon Editor - " + canonFileName(inputFile));
    }
    catch (IOException ioe)
    {
      /* We arrive here if there was a real file I/O error, or if we weren't
      careful enough about error checking and tried to seek() to a part of the
      file that doesn't exist. */

      stopFlag = true;            // stop looking at this file
      stopText = "Can't read from input file:\n" + ioe.getMessage();
    }

    /* The input file is now closed, or there was an unrecoverable I/O error.
    If we stopped because of an error, then show the error message and display
    the default icon selection. */

    if (stopFlag)                 // was there an error?
    {
      mainFrame.setTitle(PROGRAM_TITLE); // remove file name from title bar
      clearFileIcons();           // remove existing icon tabs, create new tabs
      JOptionPane.showMessageDialog(mainFrame, stopText); // show error to user
    }
  } // end of readIconFile() file


/*
  readIconUnsignedByte(), readIconUnsignedInt(), readIconUnsignedShort()

  Helper methods for readIconFile() to read specific data types.  Windows uses
  little-endian order for bytes, while Java uses big-endian.
*/
  static int readIconUnsignedByte(RandomAccessFile ramFile)
    throws IOException            // return unsigned 8-bit byte
  {
    return(((int) ramFile.read()) & BYTE_MASK);
  }

  static int readIconUnsignedInt(RandomAccessFile ramFile)
    throws IOException            // return unsigned 32-bit "double word"
  {
    return((readIconUnsignedByte(ramFile)
      | (readIconUnsignedByte(ramFile) << 8)
      | (readIconUnsignedByte(ramFile) << 16)
      | (readIconUnsignedByte(ramFile) << 24)) & 0x7FFFFFFF);
  }

  static int readIconUnsignedShort(RandomAccessFile ramFile)
    throws IOException            // return unsigned 16-bit "word"
  {
    return((readIconUnsignedByte(ramFile)
      | (readIconUnsignedByte(ramFile) << 8)) & 0x0000FFFF);
  }


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  IconEdit1  [options]  [fileName]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -e = save empty icons with no color data (not recommended)");
    System.err.println("  -p# = name of text file with 16-color palette (see sample file)");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(" + DEFAULT_LEFT + "," + DEFAULT_TOP
      + "," + MIN_WIDTH + "," + MIN_HEIGHT + ")");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println();
    System.err.println("This is a graphical application.  Only one parameter may be given on the");
    System.err.println("command line: the name of an existing icon file to open for reading.");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main IconEdit1 class.
*/
  static void userButton(ActionEvent event)
  {
    statusDialog.setText(EMPTY_STATUS); // clear previous status message
    Object source = event.getSource(); // where the event came from
    if (source == colorSample)    // user clicked on current color selection
    {
      colorDialog.setVisible(true); // show the color selection tool
    }
    else if (source == menuClearAll) // "Clear All Icons" menu item
    {
      if (JOptionPane.showConfirmDialog(mainFrame,
        "Erase all icons, all sizes?") == JOptionPane.YES_OPTION)
      {
        clearFileIcons();         // remove existing icon tabs, create new tabs
        fileChooser.setSelectedFile(new File("")); // cancel selected file name
        mainFrame.setTitle(PROGRAM_TITLE); // remove file name from title bar
      }
    }
    else if (source == menuDelete) // "Delete This Icon" menu item
    {
      int select = tabbedPane.getSelectedIndex(); // get selected icon tab
      if (select < 0)
        statusDialog.setText(
          "Can't delete selected icon if no icon tab is selected.");
      else if (JOptionPane.showConfirmDialog(mainFrame,
        "Delete this icon, this size?") == JOptionPane.YES_OPTION)
      {
        tabbedPane.remove(select); // remove tab *and* icon panel/component
      }
    }
    else if (source == menuEraseColor) // "Erase Selected Color" menu item
    {
      eraseSelectedColor();       // call someone else to do the dirty work
    }
    else if (source == menuExit)  // "Exit (Close)" menu item
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == menuFileOpen) // "Open Icon File" menu item
    {
      readIconFile(null);         // ask user for *.ICO file, then read icons
    }
    else if (source == menuFileSave) // "Save Icon File" menu item
    {
      writeIconFile();            // save all non-empty icons to new *.ICO file
    }
    else if ((source == menuGridBack) || (source == menuGridHide)
      || (source == menuGridNorm)) // "Show/Hide Gridlines" menu items
    {
      int select = tabbedPane.getSelectedIndex(); // get selected icon tab
      if (select >= 0)            // redraw current icon panel if possible
        tabbedPane.getComponentAt(select).repaint();
    }
    else if (source == menuIconClear) // "Clear This Icon" menu item
    {
      int select = tabbedPane.getSelectedIndex(); // get selected icon tab
      if (select < 0)
        statusDialog.setText(
          "Can't clear selected icon if no icon tab is selected.");
      else if (JOptionPane.showConfirmDialog(mainFrame,
        "Erase this icon, this size?") == JOptionPane.YES_OPTION)
      {
        IconEdit1Icon icon = (IconEdit1Icon) tabbedPane.getComponentAt(select);
        icon.clearThisIcon();     // clear (erase) this icon, this size
        icon.repaint();           // and redisplay icon samples
      }
    }
    else if (source == menuIconCopy) // "Copy This Icon" menu item
    {
      int select = tabbedPane.getSelectedIndex(); // get selected icon tab
      if (select < 0)
        statusDialog.setText(
          "Can't copy selected icon if no icon tab is selected.");
      else
      {
        IconEdit1Icon icon = (IconEdit1Icon) tabbedPane.getComponentAt(select);
        icon.copyThisIcon();      // copy this icon to clipboard as an image
      }
    }
    else if (source == menuIconPaste) // "Paste This Icon" menu item
    {
      int select = tabbedPane.getSelectedIndex(); // get selected icon tab
      if (select < 0)
        statusDialog.setText(
          "Can't paste selected icon if no icon tab is selected.");
      else
      {
        IconEdit1Icon icon = (IconEdit1Icon) tabbedPane.getComponentAt(select);
        icon.pasteThisIcon();     // paste image from clipboard to this icon
                                  // repaint done by pasteThisIcon() if needed
      }
    }
    else if (source == menuMoveDown) // "Move Tab Down" menu item
    {
      int select = tabbedPane.getSelectedIndex(); // get selected icon tab
      if (select < 0)
        statusDialog.setText(
          "Can't move selected icon if no icon tab is selected.");
      else if (select >= (tabbedPane.getTabCount() - 1))
        statusDialog.setText("Can't move last icon tab down any further.");
      else                        // copy selected tab to one position lower
      {
        Component component = tabbedPane.getComponentAt(select);
        String title = tabbedPane.getTitleAt(select);
        tabbedPane.removeTabAt(select); // must remove before insert, because
                                  // can't have two tabs with same component
        tabbedPane.insertTab(title, null, component, null, (select + 1));
        tabbedPane.setSelectedIndex(select + 1);
      }
    }
    else if (source == menuMoveUp) // "Move Tab Up" menu item
    {
      int select = tabbedPane.getSelectedIndex(); // get selected icon tab
      if (select < 0)
        statusDialog.setText(
          "Can't move selected icon if no icon tab is selected.");
      else if (select < 1)
        statusDialog.setText("Can't move first icon tab up any further.");
      else                        // copy selected tab to one position higher
      {
        Component component = tabbedPane.getComponentAt(select);
        String title = tabbedPane.getTitleAt(select);
        tabbedPane.removeTabAt(select); // must remove before insert, because
                                  // can't have two tabs with same component
        tabbedPane.insertTab(title, null, component, null, (select - 1));
        tabbedPane.setSelectedIndex(select - 1);
      }
    }
    else if (source == menuNew16) // create new icon tab
    {
      tabbedPane.addTab("16 x 16", new IconEdit1Icon(16));
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }
    else if (source == menuNew24) // create new icon tab
    {
      tabbedPane.addTab("24 x 24", new IconEdit1Icon(24));
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }
    else if (source == menuNew32) // create new icon tab
    {
      tabbedPane.addTab("32 x 32", new IconEdit1Icon(32));
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }
    else if (source == menuNew48) // create new icon tab
    {
      tabbedPane.addTab("48 x 48", new IconEdit1Icon(48));
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }
    else if (source == menuNew64) // create new icon tab
    {
      tabbedPane.addTab("64 x 64", new IconEdit1Icon(64));
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }
    else if (source == menuNew96) // create new icon tab
    {
      tabbedPane.addTab("96 x 96", new IconEdit1Icon(96));
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }
    else if (source == menuNew128) // create new icon tab
    {
      tabbedPane.addTab("128 x 128", new IconEdit1Icon(128));
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }
    else if (source == menuNew192) // create new icon tab
    {
      tabbedPane.addTab("192 x 192", new IconEdit1Icon(192));
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }
    else if (source == menuNew256) // create new icon tab
    {
      tabbedPane.addTab("256 x 256", new IconEdit1Icon(256));
      tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }
    else if (source == menuNewSize) // create new icon size chosen by user
    {
      createNewIconSize();        // more complicated, so call someone else
    }
    else if (source == menuShowColors) // "Show Colors" menu item
    {
      colorDialog.setVisible(true); // show the color selection tool
    }
    else if (source == menuText)  // "Set Hidden Text" menu item
    {
      String answer = JOptionPane.showInputDialog(mainFrame,
        "Enter a text string that will be hidden inside new icon files.  The current text is:",
        hiddenText);
      if (answer != null)         // null result means user cancelled dialog
        hiddenText = answer;      // remember non-null results for later
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method


/*
  writeIconFile() method

  Write all non-empty icons to a single *.ICO file.  Icons will appear in the
  same order as their tabs in the tabbed pane.  Writing is much easier than
  reading, because we just generate a stream of bytes from fixed assumptions.
*/
  static void writeIconFile()
  {
    int answer;                   // answer from a yes-no-cancel dialog box
    int bufferIndex;              // current byte index into a byte buffer
    int column, row;              // zero-based indices into color data
    boolean emptyFlag;            // true if an icon is completely transparent
    byte[] fileHeader;            // the "icon header" or beginning of file
    int fileOffset;               // offset for next image data in file
    int notEmptyCount;            // number of icons that aren't empty
    byte[][] notEmptyData;        // Microsoft image data for non-empty icons
    IconEdit1Icon[] notEmptySource; // original icon objects for non-empty icons
    File outputFile;              // user's selected output file
    int outputSize;               // number of bytes written to output file
    FileOutputStream outputStream; // our output stream for user's file
    int padBytes;                 // number of zero bytes at end of each row
    int tabCount;                 // number of icon tabs in tabbed pane
    int tabIndex;                 // index of current tab in tabbed pane
    int thisDataBytes;            // total bytes for XOR color data + AND mask
    IconEdit1Icon thisIcon;       // current icon object, filled with our data
    byte[] thisImageData;         // image data for current <thisIcon>
    int thisImageSize;            // total size of <thisImageData> in bytes

    /* End early if there are no icon tabs. */

    tabCount = tabbedPane.getTabCount(); // total number of icon tabs
    if (tabCount == 0)
    {
      JOptionPane.showMessageDialog(mainFrame,
        "There are no icon tabs open, so there is nothing to save.");
      return;                     // do nothing more, abort file saving
    }

    /* Create arrays for remembering each non-empty icon. */

    notEmptyData = new byte[tabCount][];
                                  // Microsoft image data for non-empty icons
    notEmptySource = new IconEdit1Icon[tabCount];
                                  // original icon objects for non-empty icons

    /* Go through the icon tabs looking for empty icons (fully transparent), or
    icons whose color depth is incorrect. */

    notEmptyCount = 0;            // number of tabs found to have valid data
    for (tabIndex = 0; tabIndex < tabCount; tabIndex ++)
    {
      thisIcon = (IconEdit1Icon) tabbedPane.getComponentAt(tabIndex);
                                  // get our icon object from tabbed pane

      /* Is this icon empty, and should it be ignored? */

      emptyFlag = true;           // assume that icon has no valid data
      for (row = 0; row < thisIcon.size; row ++)
      {
        for (column = 0; column < thisIcon.size; column ++)
        {
          if (thisIcon.isOpaque[row][column]) // true if a solid color
          {
            emptyFlag = false;    // any solid color means icon isn't empty
            break;                // exit early from "column" loop
          }
        }
        if (!emptyFlag)           // did we find a solid color?
          break;                  // exit early from "row" loop
      }

      /* If this icon was not empty, then it is worth saving. */

      if (allowEmptyIcons || !emptyFlag) // was this icon found to be valid?
      {
        notEmptySource[notEmptyCount] = thisIcon; // save original source

        /* Check if the colors used by the icon match the supposed color
        depth. */

        if (thisIcon.depthWarning) // already warned user about color depth?
        {
          /* Be nice: don't warn user each time he/she saves the same icon! */
        }

        else if (colorsAreWindows16(thisIcon.colors))
        {
          if (thisIcon.colorDepth != 4)
          {
            answer = JOptionPane.showConfirmDialog(mainFrame,
              ("Icon #" + (tabIndex + 1) + " at " + thisIcon.size + " x "
              + thisIcon.size
              + " should be saved with a 16-color palette.  Change this?"));
            thisIcon.depthWarning = true; // warning has been given, once only
            if (answer == JOptionPane.CANCEL_OPTION)
              return;             // do nothing more, abort file saving
            else if (answer == JOptionPane.YES_OPTION)
            {
              thisIcon.colorDepth = 4; // replace color depth
              thisIcon.depthBit4.setSelected(true); // update radio button
              thisIcon.repaint(); // some colors might display differently now
            }
          }
        }

        else if (colorsAreWebSafe(thisIcon.colors))
        {
          if (thisIcon.colorDepth != 8)
          {
            answer = JOptionPane.showConfirmDialog(mainFrame,
              ("Icon #" + (tabIndex + 1) + " at " + thisIcon.size + " x "
              + thisIcon.size
              + " should be saved with a 256-color palette.  Change this?"));
            thisIcon.depthWarning = true; // warning has been given, once only
            if (answer == JOptionPane.CANCEL_OPTION)
              return;             // do nothing more, abort file saving
            else if (answer == JOptionPane.YES_OPTION)
            {
              thisIcon.colorDepth = 8; // replace color depth
              thisIcon.depthBit8.setSelected(true); // update radio button
              thisIcon.repaint(); // some colors might display differently now
            }
          }
        }

        else                      // image should have 24-bit color
        {
          if (thisIcon.colorDepth != 24)
          {
            answer = JOptionPane.showConfirmDialog(mainFrame,
              ("Icon #" + (tabIndex + 1) + " at " + thisIcon.size + " x "
              + thisIcon.size
              + " should be saved with 24-bit RGB colors.  Change this?"));
            thisIcon.depthWarning = true; // warning has been given, once only
            if (answer == JOptionPane.CANCEL_OPTION)
              return;             // do nothing more, abort file saving
            else if (answer == JOptionPane.YES_OPTION)
            {
              thisIcon.colorDepth = 24; // replace color depth
              thisIcon.depthBit24.setSelected(true); // update radio button
              thisIcon.repaint(); // some colors might display differently now
            }
          }
        }

        /* Create the image data for this icon.  This depends heavily on the
        number of bits per pixel, except for the final AND mask (which is the
        same for all color depths).  We must correctly pad the size of each
        row to a multiple of 32 bits (4 bytes). */

        thisImageSize = 40;       // start with fixed DIB header size
        if (thisIcon.colorDepth == 4) // 4-bit color (16 colors)
        {
          thisImageSize += 16 * 4; // add one RGBQuad for each color in palette
          thisDataBytes = thisIcon.size * (((thisIcon.size + 7) / 8) * 4);
                                  // 4 bits of XOR color data per pixel
        }
        else if (thisIcon.colorDepth == 8) // 8-bit color (256 colors)
        {
          thisImageSize += 256 * 4; // add one RGBQuad for each color in palette
          thisDataBytes = thisIcon.size * (((thisIcon.size + 3) / 4) * 4);
                                  // one byte of XOR color data per pixel
        }
        else                      // assume anything else is 24-bit RGB color
        {
          thisImageSize += 0;     // no RGBQuad, no palette for 24-bit color
          thisDataBytes = thisIcon.size * ((((thisIcon.size * 3) + 3) / 4) * 4);
                                  // three bytes of XOR color data per pixel
        }
        thisDataBytes += thisIcon.size * (((thisIcon.size + 31) / 32) * 4);
                                  // AND mask rounded up to 32 bits per row
        thisImageSize += thisDataBytes; // add size of XOR data + AND mask

        /* We know the correct size for the image data, and we know the values
        that go in the bitmap header, so use common code for the header. */

        thisImageData = new byte[thisImageSize]; // allocate image buffer

        bufferIndex = writeIconUnsignedInt(thisImageData, 0, 40);
                                  // write biSize
        bufferIndex = writeIconUnsignedInt(thisImageData, bufferIndex,
          thisIcon.size);         // write biWidth
        bufferIndex = writeIconUnsignedInt(thisImageData, bufferIndex,
          (thisIcon.size * 2));   // write biHeight
        bufferIndex = writeIconUnsignedShort(thisImageData, bufferIndex, 1);
                                  // write biPlanes
        bufferIndex = writeIconUnsignedShort(thisImageData, bufferIndex,
          thisIcon.colorDepth);   // write biBitCount
        bufferIndex = writeIconUnsignedInt(thisImageData, bufferIndex, 0);
                                  // write biCompression
        bufferIndex = writeIconUnsignedInt(thisImageData, bufferIndex,
          thisDataBytes);         // write biSizeImage
        bufferIndex = writeIconUnsignedInt(thisImageData, bufferIndex, 0);
                                  // write biXPelsPerMeter
        bufferIndex = writeIconUnsignedInt(thisImageData, bufferIndex, 0);
                                  // write biYPelsPerMeter
        bufferIndex = writeIconUnsignedInt(thisImageData, bufferIndex, 0);
                                  // write biClrUsed
        bufferIndex = writeIconUnsignedInt(thisImageData, bufferIndex, 0);
                                  // write biClrImportant

        /* We have to split off again into separate sections to write the
        palettes and XOR color data. */

        if (thisIcon.colorDepth == 4) // 4-bit color (16 colors)
        {
          /* Write standard 4-bit palette (16 colors). */

          for (int i = 0; i < colorPalette16.length; i ++)
          {
            bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
              colorPalette16[i][2]); // write rgbBlue
            bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
              colorPalette16[i][1]); // write rgbGreen
            bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
              colorPalette16[i][0]); // write rgbRed
            bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex, 0);
                                  // write rgbReserved
          }

          /* Write the color pixels by shifting RGB colors to their closest
          index in the standard 16-color palette color.  We don't do customized
          palettes ... yet.  Each nibble (4 bits) in a written data byte has
          the index for one pixel.  The following code assumes that the icon
          size is positive.  Rows are padded to 32 bits (4 bytes) with extra
          zero bytes, if necessary. */

          padBytes = (((thisIcon.size + 7) / 8) * 4)
            - ((thisIcon.size + 1) / 2); // null bytes added to end of each row
          for (row = (thisIcon.size - 1); row >= 0; row --) // bottom-to-top
          {
            int bitShift = 4;     // bit shift for first column
            int byteValue = 0;    // logical OR of two palette indexes
            for (column = 0; column < thisIcon.size; column ++)
            {
              if (bitShift < 0)   // have we already filled one byte?
              {
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  byteValue);     // write one byte of the XOR color data
                bitShift = 4;     // reset to high-order nibble in byte
                byteValue = 0;    // clear palette indexes (zero is black)
              }

              if (thisIcon.isOpaque[row][column]) // is this a solid color?
                byteValue |= getCloseWin16Index(thisIcon.colors[row][column])
                  << bitShift;    // shift 16-color index to correct nibble

              bitShift -= 4;      // next column goes four bits lower
            }
            bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
              byteValue);         // there is always one unfinished byte
            for (int i = 0; i < padBytes; i ++) // pad row to 32 bits (4 bytes)
              bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex, 0);
          }
        }

        else if (thisIcon.colorDepth == 8) // 8-bit color (256 colors)
        {
          /* Write standard 8-bit palette (256 colors).  The "web safe" colors
          use the first 216 palette entries (6x6x6), and we zero fill the rest
          of the table with black. */

          for (int green = 0; green <= 255; green += 51)
          {
            for (int red = 0; red <= 255; red += 51)
              for (int blue = 0; blue <= 255; blue += 51)
              {
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  blue);          // write rgbBlue
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  green);         // write rgbGreen
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  red);           // write rgbRed
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  0);             // write rgbReserved
              }
          }
          for (int i = 0; i < 160; i ++) // plus 40 black RGBquad to total 256
            bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex, 0);

          /* Write the color pixels by shifting RGB colors to their closest
          "web safe" color.  We don't do customized palettes ... yet. */

          padBytes = (((thisIcon.size + 3) / 4) * 4) - thisIcon.size;
                                  // null bytes added to end of each row
          for (row = (thisIcon.size - 1); row >= 0; row --) // bottom-to-top
          {
            for (column = 0; column < thisIcon.size; column ++)
            {
              int rgbIndex;       // palette index that we will write

              if (thisIcon.isOpaque[row][column]) // is this a solid color?
              {
                rgbIndex = ((int) ((thisIcon.colors[row][column].getGreen()
                  + 25.5) / 51.0)) * 36;
                rgbIndex += ((int) ((thisIcon.colors[row][column].getRed()
                  + 25.5) / 51.0)) * 6;
                rgbIndex += (int) ((thisIcon.colors[row][column].getBlue()
                  + 25.5) / 51.0);
              }
              else                // no, pixel is transparent
                rgbIndex = 0;     // an index of zero is the color black

              bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                rgbIndex);
            }
            for (int i = 0; i < padBytes; i ++) // pad row to 32 bits (4 bytes)
              bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex, 0);
          }
        }

        else                      // assume anything else is 24-bit RGB color
        {
          /* RGB 24-bit color doesn't have a palette.  Write only the three
          BGR bytes for this color.  Yes, BGR: blue-green-red order. */

          padBytes = ((((thisIcon.size * 3) + 3) / 4) * 4)
            - (thisIcon.size * 3); // null bytes added to end of each row
          for (row = (thisIcon.size - 1); row >= 0; row --) // bottom-to-top
          {
            for (column = 0; column < thisIcon.size; column ++)
            {
              if (thisIcon.isOpaque[row][column]) // is this a solid color?
              {
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  thisIcon.colors[row][column].getBlue());
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  thisIcon.colors[row][column].getGreen());
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  thisIcon.colors[row][column].getRed());
              }
              else                // no, pixel is transparent
              {
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  0);             // write RGB (0, 0, 0) for black
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  0);
                bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                  0);
              }
            }
            for (int i = 0; i < padBytes; i ++) // pad row to 32 bits (4 bytes)
              bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex, 0);
          }
        }

        /* Append the AND mask to the end of the image data created above.
        There is a one bit for each pixel that is transparent, and zero bits
        for pixels that are opaque.  The following code assumes that the icon
        size is positive.  Rows are padded to 32 bits (4 bytes) with extra zero
        bytes, if necessary. */

        padBytes = (((thisIcon.size + 31) / 32) * 4)
          - ((thisIcon.size + 7) / 8); // null bytes added to end of each row
        for (row = (thisIcon.size - 1); row >= 0; row --) // bottom-to-top
        {
          int bitShift = 7;       // bit shift for first column
          int byteValue = 0;      // logical OR of all transparency flags
          for (column = 0; column < thisIcon.size; column ++)
          {
            if (bitShift < 0)     // have we already filled one byte?
            {
              bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
                byteValue);       // write one byte of the AND mask
              bitShift = 7;       // reset to high-order bit in byte
              byteValue = 0;      // reset to all opaque (not transparent)
            }

            if (thisIcon.isOpaque[row][column] == false) // if not opaque
              byteValue |= (1 << bitShift); // set transparency flag

            bitShift --;          // next column goes one bit lower
          }
          bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex,
            byteValue);           // there is always one unfinished byte
          for (int i = 0; i < padBytes; i ++) // pad row to 32 bits (4 bytes)
            bufferIndex = writeIconUnsignedByte(thisImageData, bufferIndex, 0);
        }

        /* Check that we filled the image data buffer completely.  This is a
        test of correct program logic, not something that involves the user. */

        if (bufferIndex != thisImageData.length)
        {
          System.err.println("Icon #" + (tabIndex + 1)
            + " image data has wrong size: bufferIndex " + bufferIndex
            + ", thisImageData.length " + thisImageData.length + ".");
        }

        /* Save this image data buffer for later. */

        notEmptyData[notEmptyCount] = thisImageData;

        /* Increment total number of non-empty icons.  We didn't do this
        earlier because we use <notEmptyCount> as an index into data that we
        collect, and will use later to generate a combined byte stream. */

        notEmptyCount ++;         // increment number of icons worth saving

      } // end of if icon not empty

    } // end of loop for each icon tab

    /* Were there any icons worth saving? */

    if (notEmptyCount == 0)
    {
      JOptionPane.showMessageDialog(mainFrame,
        "All icons are empty (completely transparent).  There is nothing to save.");
      return;                     // do nothing more, abort file saving
    }

    /* Create a file icon header, to go with the byte streams we built for each
    non-empty icon. */

    fileHeader = new byte[(notEmptyCount * 16) + 6];
                                  // 6 bytes for icon header, 16 each entry
    bufferIndex = writeIconUnsignedShort(fileHeader, 0, 0); // write idReserved
    bufferIndex = writeIconUnsignedShort(fileHeader, bufferIndex, 1);
                                  // write idType
    bufferIndex = writeIconUnsignedShort(fileHeader, bufferIndex,
      notEmptyCount);             // write idCount

    fileOffset = fileHeader.length; // data for first icon image goes here
    for (int i = 0; i < notEmptyCount; i ++)
    {
      bufferIndex = writeIconUnsignedByte(fileHeader, bufferIndex,
        (notEmptySource[i].size & BYTE_MASK)); // write bWidth (256 as zero)
      bufferIndex = writeIconUnsignedByte(fileHeader, bufferIndex,
        (notEmptySource[i].size & BYTE_MASK)); // write bHeight (256 as zero)
      bufferIndex = writeIconUnsignedByte(fileHeader, bufferIndex,
        ((notEmptySource[i].colorDepth == 4) ? 16 : 0)); // write bColorCount
      bufferIndex = writeIconUnsignedByte(fileHeader, bufferIndex, 0);
                                  // write bReserved
      bufferIndex = writeIconUnsignedShort(fileHeader, bufferIndex, 1);
                                  // write wPlanes
      bufferIndex = writeIconUnsignedShort(fileHeader, bufferIndex,
        notEmptySource[i].colorDepth); // write wBitCount
      bufferIndex = writeIconUnsignedInt(fileHeader, bufferIndex,
        notEmptyData[i].length);  // write dwBytesInRes
      bufferIndex = writeIconUnsignedInt(fileHeader, bufferIndex,
        fileOffset);              // write dwImageOffset

      fileOffset += notEmptyData[i].length; // offset for next image data
    }

    /* Similar to the data for each icon image, check that we filled the icon
    file header completely. */

    if (bufferIndex != fileHeader.length)
    {
      System.err.println("Icon file header has wrong size: bufferIndex "
        + bufferIndex + ", fileHeader.length " + fileHeader.length + ".");
    }

    /* We've done a huge amount of work behind the scenes, which will complete
    quickly because icons are so small.  Now ask the user for an output file
    name. */

    fileChooser.setDialogTitle("Save Icon File...");
    if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    outputFile = fileChooser.getSelectedFile(); // get user's output file

    /* Write bytes to output file. */

    try                           // to catch file I/O errors
    {
      if (canWriteFile(outputFile)) // if writing this file seems safe
      {
        outputStream = new FileOutputStream(outputFile);
        outputStream.write(fileHeader); // write icon file header
        outputSize = fileHeader.length; // total bytes written so far
        for (int i = 0; i < notEmptyCount; i ++)
        {
          outputStream.write(notEmptyData[i]); // write image data each icon
          outputSize += notEmptyData[i].length; // increment bytes written
        }

        /* We pad the file so that it ends on a 16-byte boundary.  This is not
        a requirement of any standard or specification. */

        for (int i = ((((outputSize + 15) / 16) * 16) - outputSize); i > 0;
          i --)
        {
          outputStream.write(0x00); // append one null byte
          outputSize ++;          // one more byte written
        }

        /* If the user gave us some hidden text to include in the file (such as
        a copyright notice), then put this at the end of the file.  It might be
        possible to put text between the icon directory and the icon image
        data, but I don't want to risk upsetting some program that assumes the
        image data is contiguous. */

        if (hiddenText.length() > 0) // is there a hidden text string to write?
        {
          byte[] hiddenBytes = hiddenText.getBytes(); // convert string to bytes
          outputStream.write(hiddenBytes); // write hidden text as bytes
          outputSize += hiddenBytes.length; // increment total bytes written

          for (int i = ((((outputSize + 15) / 16) * 16) - outputSize); i > 0;
            i --)
          {
            outputStream.write(0x00); // append one null byte
            outputSize ++;        // one more byte written
          }
        }

        /* Close the output file.  We are finished writing! */

        outputStream.close();     // try to close output file
        mainFrame.setTitle("Icon Editor - " + canonFileName(outputFile));
      }
    }
    catch (IOException ioe)
    {
      JOptionPane.showMessageDialog(mainFrame, ("Can't write to output file: "
        + ioe.getMessage()));
    }
  } // end of writeIconFile() file


/*
  writeIconUnsignedByte(), writeIconUnsignedInt(), writeIconUnsignedShort()

  Helper methods for writeIconFile() to put specific data types into a byte
  buffer.  The caller gives us an index into the buffer, which we update and
  return as our result.
*/
  static int writeIconUnsignedByte(byte[] buffer, int index, int value)
  {
    buffer[index] = (byte) (value & BYTE_MASK);
    return(index + 1);            // give caller updated buffer index
  }

  static int writeIconUnsignedInt(byte[] buffer, int index, int value)
  {
    buffer[index + 0] = (byte) (value & BYTE_MASK);
    buffer[index + 1] = (byte) ((value >> 8) & BYTE_MASK);
    buffer[index + 2] = (byte) ((value >> 16) & BYTE_MASK);
    buffer[index + 3] = (byte) ((value >> 24) & BYTE_MASK);
    return(index + 4);            // give caller updated buffer index
  }

  static int writeIconUnsignedShort(byte[] buffer, int index, int value)
  {
    buffer[index + 0] = (byte) (value & BYTE_MASK);
    buffer[index + 1] = (byte) ((value >> 8) & BYTE_MASK);
    return(index + 2);            // give caller updated buffer index
  }

} // end of IconEdit1 class

// ------------------------------------------------------------------------- //

/*
  IconEdit1Draw class

  This class draws a single icon in whatever size of panel we are given.
*/

class IconEdit1Draw extends JPanel
  implements MouseListener, MouseMotionListener
{
  /* instance variables */

  int gridBorder;                 // width of outside border in pixels
  int gridLeft, gridTop;          // margins to keep icon centered in panel
  int gridSize;                   // calculated size for each icon square, but
                                  // not including gridlines
  int gridStep;                   // pixel increment from start of one icon
                                  // square to start of next, including any
                                  // gridlines
  int gridTotal;                  // temporary for total height/width of icon
  int gridWidth;                  // width of internal gridlines in pixels
  IconEdit1Icon iconSource;       // link back to original icon and its data
  int mouseButton;                // mouse button actually pressed (not drag)
  boolean mouseClicks;            // true if we listen to mouse clicks

  /* constructor */

  public IconEdit1Draw(
    IconEdit1Icon newIcon,        // where our icon data comes from
    boolean newMouse)             // true if we listen to mouse clicks
  {
    iconSource = newIcon;         // remember where we get our data from
    mouseClicks = newMouse;       // remember if we listen to mouse clicks
    if (mouseClicks)              // should we listen to mouse clicks, drags?
    {
      mouseButton = MouseEvent.NOBUTTON; // no mouse button pressed yet
      this.addMouseListener((MouseListener) this);
      this.addMouseMotionListener((MouseMotionListener) this);
    }
    this.setBackground(IconEdit1.BACKGROUND); // background color for JPanel
  }


/*
  doMouseEvent() and mouseClicked() through mouseReleased()

  Mouse listener for both clicks and drags.  Avoid updating (redrawing) the
  display unless something visible has changed, because the motion listener is
  called frequently.  We remember <mouseButton> from the initial press because
  during a drag operation, getButton() always reports NOBUTTON.
*/
  public void doMouseEvent(MouseEvent event)
  {
    Color color;                  // temporary color during color selection
    int column, row;              // zero-based indices into icon data
    boolean update;               // true of we need to update (redraw) display

    column = (event.getX() - gridLeft - gridBorder) / gridStep;
                                  // convert x-coordinate to column number
    row = (event.getY() - gridTop - gridBorder) / gridStep;
                                  // convert y-coordinate to row number
    update = false;               // assume that nothing changes
    if ((column < 0) || (column >= iconSource.size) || (row < 0)
      || (row >= iconSource.size))
    {
      /* Ignore the mouse when it is not on an icon square. */
    }
    else if (event.isControlDown() // control click means clear to transparent
      || (mouseButton == MouseEvent.BUTTON2) // push scroll wheel on PC
      || (mouseButton == MouseEvent.BUTTON3)) // right button on PC
    {
      if ((iconSource.colors[row][column].equals(Color.BLACK) == false)
        || (iconSource.isOpaque[row][column] == true))
      {
        iconSource.colors[row][column] = Color.BLACK; // should be black/zero
        iconSource.isOpaque[row][column] = false; // mark transparent square
        update = true;            // display has changed
      }
    }
    else if (event.isShiftDown()) // shift click means select this color
    {
      if (iconSource.isOpaque[row][column]) // does square have solid color?
        color = iconSource.colors[row][column]; // yes, select solid color
      else                        // transparent squares don't have real color
        color = iconSource.iconBackground; // assume background color

      if (color.equals(IconEdit1.colorChooser.getColor()) == false)
                                  // if the selection color will change
        IconEdit1.colorChooser.setColor(color); // will redraw, fire change
                                  // event, which updates other variables
    }
    else                          // normal click means set to current color
    {
      if ((iconSource.colors[row][column].equals(IconEdit1.selectColor)
        == false) || (iconSource.isOpaque[row][column] == false))
      {
        iconSource.colors[row][column] = IconEdit1.selectColor;
        iconSource.isOpaque[row][column] = true; // normal click is solid color
        update = true;            // display has changed
      }
    }

    if (update)                   // did anything visible change on display?
    {
      iconSource.bigIcon.repaint(); // repaint ourself in correct context
      iconSource.smallIcon.repaint();
    }
  } // end of doMouseEvent() method

  public void mouseClicked(MouseEvent event)
  {
    mouseButton = event.getButton(); // remember actual mouse button pushed
    doMouseEvent(event);          // common processing for all mouse actions
  }
  public void mouseDragged(MouseEvent event) { doMouseEvent(event); }
  public void mouseEntered(MouseEvent event) { /* do nothing */ }
  public void mouseExited(MouseEvent event) { /* do nothing */ }
  public void mouseMoved(MouseEvent event) { /* do nothing */ }
  public void mousePressed(MouseEvent event)
  {
    mouseButton = event.getButton(); // remember actual mouse button pushed
  }
  public void mouseReleased(MouseEvent event) { /* do nothing */ }


/*
  paintComponent() method

  This is the "paint" method for a Java Swing component.
*/
  protected void paintComponent(Graphics context)
  {
    /* Erase the entire component region with the correct background color. */

//  context.clearRect(0, 0, getWidth(), getHeight()); // wrong color (white)
    context.setColor(this.getBackground());
    context.fillRect(0, 0, getWidth(), getHeight());

    /* Figure out how many drawing pixels we can use for each pixel (square) in
    the icon.  Allow for internal gridlines and outside borders. */

    if (IconEdit1.menuGridHide.isSelected()) // hide borders and gridlines?
    {
      gridBorder = gridWidth = 0; // remove outside border, remove gridlines
      gridStep = Math.min((this.getHeight() / iconSource.size),
        (this.getWidth() / iconSource.size)); // increment between icon squares
      gridStep = Math.max(gridStep, 1); // need at least one pixel per square
      gridSize = gridStep;        // and size of each icon square is the same
      gridTotal = iconSource.size * gridStep; // total height/width of icon
      gridLeft = (this.getWidth() - gridTotal) / 2; // to keep icon centered
      gridTop = (this.getHeight() - gridTotal) / 2;
    }
    else                          // user wants borders and gridlines
    {
      gridBorder = (Math.min(this.getHeight(), this.getWidth()) >
        (6 * iconSource.size)) ? 2 : 1; // desired width of outside border
      gridWidth = 1;              // desired width of internal lines in pixels
      gridStep = Math.min(        // assume room for gridlines, calculate step
        ((this.getHeight() + gridWidth - (2 * gridBorder)) / iconSource.size),
        ((this.getWidth() + gridWidth - (2 * gridBorder)) / iconSource.size));
      gridStep = Math.max(gridStep, 1); // need at least one pixel per square
      gridSize = gridStep - gridWidth; // pixel size for each icon square
      if (gridSize < 8)           // not enough room for gridlines, only border
      {
        gridWidth = 0;            // turn off internal gridlines
        gridStep = Math.min(      // recalculate step size for border only
          ((this.getHeight() - (2 * gridBorder)) / iconSource.size),
          ((this.getWidth() - (2 * gridBorder)) / iconSource.size));
        gridStep = Math.max(gridStep, 1); // still need one pixel per square
        gridSize = gridStep;      // and size of each icon square is the same
      }
      gridTotal = (2 * gridBorder) + (iconSource.size * gridStep) - gridWidth;
                                  // total height/width of displayed icon
      gridLeft = (this.getWidth() - gridTotal) / 2; // to keep icon centered
      gridTop = (this.getHeight() - gridTotal) / 2;

      /* Fill the region that we will use for the icon sample with the border
      or gridline color.  The icon squares are drawn later, and anything that
      doesn't get replaced by icon squares becomes a border or gridline.  This
      is *much* easier than drawing each line individually! */

      if (IconEdit1.menuGridBack.isSelected()) // use transparency background?
        context.setColor(iconSource.iconBackground); // yes, be transparent
      else
        context.setColor(IconEdit1.GRID_COLOR); // no, use default grid color
      context.fillRect(gridLeft, gridTop, gridTotal, gridTotal); // flood fill
    }

    /* Draw each square from the icon using our calculated number of pixels per
    square.  Transparent squares take on the current background color.  */

    for (int row = 0; row < iconSource.size; row ++)
    {
      for (int column = 0; column < iconSource.size; column ++)
      {
        if (iconSource.isOpaque[row][column])
          context.setColor(IconEdit1.getCloseColor(iconSource.colorDepth,
            iconSource.colors[row][column]));
        else
          context.setColor(iconSource.iconBackground);

        context.fillRect((gridLeft + gridBorder + (gridStep * column)),
          (gridTop + gridBorder + (gridStep * row)), gridSize, gridSize);
      }
    }
  } // end of paintComponent() method

} // end of IconEdit1Draw class

// ------------------------------------------------------------------------- //

/*
  IconEdit1Filter class

  This class limits the files shown in the file open dialog box to icon files.
*/

class IconEdit1Filter extends javax.swing.filechooser.FileFilter
                implements java.io.FileFilter // not the same as filechooser.*
{
  /* empty constructor */

  public IconEdit1Filter() { }

  /* file filter: accept files of given types */

  public boolean accept(File givenFile)
  {
    String fileName = givenFile.getName().toLowerCase(); // lowercase file name
    return(givenFile.isDirectory() || fileName.endsWith(".ico"));
  }

  /* file filter: return description of files that we accept */

  public String getDescription()
  {
    return("Icon Files (*.ICO)");
  }

} // end of IconEdit1Filter class

// ------------------------------------------------------------------------- //

/*
  IconEdit1Icon class

  This class stores data about one icon, and draws the icon's tabbed panel on
  demand.
*/
class IconEdit1Icon extends JPanel
                    implements ActionListener, ChangeListener, Transferable
{
  /* constants */

  final Color[] backColors = {IconEdit1.BACKGROUND, Color.BLACK, Color.RED,
    Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.YELLOW,
    Color.WHITE};                 // background choices for icon transparency

  /* class variables */

  static Color[][] clipColors;    // colors that generated <clipImage>
  static int clipDepth;           // color depth that generated <clipImage>
  static BufferedImage clipImage = null; // image received or sent to clipboard
  static boolean[][] clipOpaque;  // transparency that generated <clipImage>

  /* instance variables */

  JSlider backSlider;             // slider control for icon background color
  IconEdit1Draw bigIcon, smallIcon; // big and small sample displays of icon
  int colorDepth;                 // color depth in pixels (4, 8, or 24)
  Color[][] colors;               // array for RGB color values
  JRadioButton depthBit4, depthBit8, depthBit24; // buttons for color depth
  ButtonGroup depthGroup;         // grouping of radio buttons for color depth
  boolean depthWarning;           // true if we've already warned user about
                                  // incorrect color depth for this icon
  Color iconBackground;           // variable background for icon samples
  JButton iconClear, iconCopy, iconPaste; // action buttons
  boolean isOpaque[][];           // false for transparent, true for solid
  int size;                       // horizontal and vertical size in pixels

  /* constructor */

  public IconEdit1Icon(int newSize)
  {
    super();                      // initialize our superclass first (JPanel)
    colorDepth = 8;               // default to 8-bit color (256 colors)
    depthWarning = false;         // no warning issued yet about color depth
    iconBackground = backColors[0]; // first color is default background
    size = newSize;               // save caller's size in pixels

    /* Allocate space for the red-green-blue color values. */

    colors = new Color[size][size]; // allocate array for color values
    isOpaque = new boolean[size][size]; // allocate transparency flags

    /* Create the layout for this icon panel.  One over-sized icon goes on the
    left, with options and an exact-sized icon on the right. */

    Box panel1 = Box.createVerticalBox(); // for options on right side
    panel1.setBackground(IconEdit1.BACKGROUND);
    panel1.add(Box.createVerticalStrut(5));

    JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
    panel2.setBackground(IconEdit1.BACKGROUND);

    iconClear = new JButton("Clear");
    iconClear.addActionListener((ActionListener) this);
//  iconClear.setBackground(IconEdit1.BACKGROUND);
    if (IconEdit1.buttonFont != null) iconClear.setFont(IconEdit1.buttonFont);
    iconClear.setToolTipText("Clear icon, this size only.");
    panel2.add(iconClear);

    iconCopy = new JButton("Copy");
    iconCopy.addActionListener((ActionListener) this);
//  iconCopy.setBackground(IconEdit1.BACKGROUND);
    if (IconEdit1.buttonFont != null) iconCopy.setFont(IconEdit1.buttonFont);
    iconCopy.setToolTipText("Copy icon to clipboard, this size only.");
    panel2.add(iconCopy);

    iconPaste = new JButton("Paste");
    iconPaste.addActionListener((ActionListener) this);
//  iconPaste.setBackground(IconEdit1.BACKGROUND);
    if (IconEdit1.buttonFont != null) iconPaste.setFont(IconEdit1.buttonFont);
    iconPaste.setToolTipText("Paste icon from clipboard, this size only.");
    panel2.add(iconPaste);
    panel1.add(panel2);
    panel1.add(Box.createVerticalStrut(15));

    JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    panel3.setBackground(IconEdit1.BACKGROUND);
    JLabel label12 = new JLabel("colors: ");
    if (IconEdit1.buttonFont != null) label12.setFont(IconEdit1.buttonFont);
    label12.setToolTipText("Color depth for icon sample and when saving file.");
    panel3.add(label12);
    depthGroup = new ButtonGroup();

    depthBit4 = new JRadioButton("16 ", false);
    depthBit4.addActionListener((ActionListener) this);
    depthBit4.setBackground(IconEdit1.BACKGROUND);
    if (IconEdit1.buttonFont != null) depthBit4.setFont(IconEdit1.buttonFont);
    depthBit4.setToolTipText("Windows 4-bit palette (16 colors)");
    depthGroup.add(depthBit4);
    panel3.add(depthBit4);

    depthBit8 = new JRadioButton("256 ", true);
    depthBit8.addActionListener((ActionListener) this);
    depthBit8.setBackground(IconEdit1.BACKGROUND);
    if (IconEdit1.buttonFont != null) depthBit8.setFont(IconEdit1.buttonFont);
    depthBit8.setToolTipText("web safe 8-bit palette (256 colors)");
    depthGroup.add(depthBit8);
    panel3.add(depthBit8);

    depthBit24 = new JRadioButton("RGB", false);
    depthBit24.addActionListener((ActionListener) this);
    depthBit24.setBackground(IconEdit1.BACKGROUND);
    if (IconEdit1.buttonFont != null) depthBit24.setFont(IconEdit1.buttonFont);
    depthBit24.setToolTipText("RGB 24-bit color (millions)");
    depthGroup.add(depthBit24);
    panel3.add(depthBit24);
    panel1.add(panel3);
    panel1.add(Box.createVerticalStrut(15));

    JPanel panel4 = new JPanel(new BorderLayout(0, 0));
    panel4.setBackground(IconEdit1.BACKGROUND);
    panel4.add(Box.createHorizontalStrut(30), BorderLayout.WEST);
    backSlider = new JSlider(0, (backColors.length - 1), 0);
    backSlider.addChangeListener((ChangeListener) this);
    backSlider.setBackground(IconEdit1.BACKGROUND);
    backSlider.setMajorTickSpacing(1);
    backSlider.setPaintTicks(false); // not necessary with labels present
    backSlider.setSnapToTicks(true);
    Hashtable label19 = new Hashtable(); // put short color labels on slider

    JLabel label20 = new JLabel("A", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label20.setFont(IconEdit1.buttonFont);
    label20.setForeground(Color.GRAY);
//  label20.setToolTipText("gray"); // has no effect in JRE 1.4 to 7
    label19.put(new Integer(0), label20);

    JLabel label21 = new JLabel("K", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label21.setFont(IconEdit1.buttonFont);
    label21.setForeground(Color.BLACK);
//  label21.setToolTipText("black");
    label19.put(new Integer(1), label21);

    JLabel label22 = new JLabel("R", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label22.setFont(IconEdit1.buttonFont);
    label22.setForeground(Color.RED);
//  label22.setToolTipText("red");
    label19.put(new Integer(2), label22);

    JLabel label23 = new JLabel("B", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label23.setFont(IconEdit1.buttonFont);
    label23.setForeground(Color.BLUE);
//  label23.setToolTipText("blue");
    label19.put(new Integer(3), label23);

    JLabel label24 = new JLabel("M", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label24.setFont(IconEdit1.buttonFont);
    label24.setForeground(Color.MAGENTA);
//  label24.setToolTipText("magenta");
    label19.put(new Integer(4), label24);

    JLabel label25 = new JLabel("G", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label25.setFont(IconEdit1.buttonFont);
    label25.setForeground(Color.GREEN);
//  label25.setToolTipText("green");
    label19.put(new Integer(5), label25);

    JLabel label26 = new JLabel("C", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label26.setFont(IconEdit1.buttonFont);
    label26.setForeground(Color.CYAN);
//  label26.setToolTipText("cyan");
    label19.put(new Integer(6), label26);

    JLabel label27 = new JLabel("Y", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label27.setFont(IconEdit1.buttonFont);
    label27.setForeground(Color.YELLOW);
//  label27.setToolTipText("yellow");
    label19.put(new Integer(7), label27);

    JLabel label28 = new JLabel("W", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label28.setFont(IconEdit1.buttonFont);
    label28.setForeground(Color.WHITE);
//  label28.setToolTipText("white");
    label19.put(new Integer(8), label28);

    backSlider.setLabelTable(label19);
    backSlider.setPaintLabels(true);
    panel4.add(backSlider, BorderLayout.CENTER);
    panel4.add(Box.createHorizontalStrut(30), BorderLayout.EAST);
    panel1.add(panel4);
    panel1.add(Box.createVerticalStrut(5));

    JPanel panel5 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    panel5.setBackground(IconEdit1.BACKGROUND);
    JLabel label31 = new JLabel("transparency background", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label31.setFont(IconEdit1.buttonFont);
    label31.setToolTipText("Change background color to view transparent icons.");
    panel5.add(label31);
    panel1.add(panel5);
    panel1.add(Box.createVerticalStrut(22));

    JPanel panel6 = new JPanel(new BorderLayout(0, 0)); // exact size preview
    panel6.setBackground(IconEdit1.BACKGROUND);
    panel6.add(Box.createHorizontalStrut(size + 6), BorderLayout.NORTH);
    panel6.add(Box.createVerticalStrut(size + 6), BorderLayout.WEST);
    smallIcon = new IconEdit1Draw(this, false); // draw this icon at exact size
    panel6.add(smallIcon, BorderLayout.CENTER);

    JPanel panel7 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    panel7.setBackground(IconEdit1.BACKGROUND);
    panel7.add(panel6);
    panel1.add(panel7);
    panel1.add(Box.createVerticalStrut(7));

    JPanel panel8 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    panel8.setBackground(IconEdit1.BACKGROUND);
    JLabel label32 = new JLabel("icon sample, actual size", JLabel.CENTER);
    if (IconEdit1.buttonFont != null) label32.setFont(IconEdit1.buttonFont);
    panel8.add(label32);
    panel1.add(panel8);

    JPanel panel9 = new JPanel(new BorderLayout(0, 0));
    panel9.setBackground(IconEdit1.BACKGROUND);
    panel9.add(panel1, BorderLayout.NORTH);

    this.setBackground(IconEdit1.BACKGROUND);
    this.setLayout(new BorderLayout(5, 5)); // layout manager for this panel
    this.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
    this.add(Box.createHorizontalStrut(5), BorderLayout.WEST);
    bigIcon = new IconEdit1Draw(this, true); // draw icon as big as possible
    this.add(bigIcon, BorderLayout.CENTER);
    this.add(panel9, BorderLayout.EAST);
    this.add(Box.createVerticalStrut(3), BorderLayout.SOUTH);

    /* Now that everything has been created, set the icon panel to its initial
    state. */

    this.clearThisIcon();         // set or reset icon panel to initial values

  } // end of IconEdit1Icon constructor

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == depthBit4)      // select 4-bit color (16 colors)
    {
      colorDepth = 4;             // save new color depth
      this.repaint();             // and redisplay icon samples
    }
    else if (source == depthBit8) // select 8-bit color (256 colors)
    {
      colorDepth = 8;             // save new color depth
      this.repaint();             // and redisplay icon samples
    }
    else if (source == depthBit24) // select 24-bit color (millions)
    {
      colorDepth = 24;            // save new color depth
      this.repaint();             // and redisplay icon samples
    }
    else if (source == iconClear) // clear this icon size to initial state
    {
      IconEdit1.statusDialog.setText(IconEdit1.EMPTY_STATUS);
                                  // clear any previous status message
      if (JOptionPane.showConfirmDialog(IconEdit1.mainFrame,
        "Erase this icon, this size?") == JOptionPane.YES_OPTION)
      {
        clearThisIcon();          // set or reset icon panel to initial values
        this.repaint();           // and redisplay icon samples
      }
    }
    else if (source == iconCopy)  // copy this icon size to the clipboard
    {
      IconEdit1.statusDialog.setText(IconEdit1.EMPTY_STATUS);
                                  // clear any previous status message
      copyThisIcon();             // copy this icon to clipboard as an image
                                  // no need to do a repaint() here
    }
    else if (source == iconPaste) // paste clipboard to this icon size
    {
      IconEdit1.statusDialog.setText(IconEdit1.EMPTY_STATUS);
                                  // clear any previous status message
      pasteThisIcon();            // paste image from clipboard to this icon
                                  // repaint done by pasteThisIcon() if needed
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in actionPerformed(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of actionPerformed() method

  /* Set or reset this icon panel to its initial values. */

  void clearThisIcon()
  {
    backSlider.setValue(0);       // first color is default background
    colorDepth = 8;               // reset to 8-bit color (256 colors)
    depthBit8.setSelected(true);  // select radio button for 8-bit color
    iconBackground = backColors[0]; // first color is default background
    for (int row = 0; row < size; row ++)
      for (int column = 0; column < size; column ++)
      {
        colors[row][column] = Color.BLACK; // RGB (0, 0, 0) for opaque colors
        isOpaque[row][column] = false; // but default to transparent pixels
      }
  }

  /* Copy this icon to the clipboard as an image.  The Java 1.4 clipboard
  doesn't retain transparency information, so all pixels become solid colors
  (opaque). */

  void copyThisIcon()
  {
    int column, row;              // zero-based indices into icon data

    /* Create a buffered image from our internal pixel data. */

    clipColors = new Color[size][size]; // our saved copy of color array
    clipDepth = colorDepth;       // our saved copy of color depth
    clipImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    clipOpaque = new boolean[size][size]; // saved copy of transparency flags

    for (row = 0; row < size; row ++)
      for (column = 0; column < size; column ++)
      {
        clipColors[row][column] = colors[row][column]; // copy color array
        clipOpaque[row][column] = isOpaque[row][column]; // copy transparency

        if (isOpaque[row][column]) // is this a solid color?
          clipImage.setRGB(column, row, colors[row][column].getRGB());
        else                      // use background for transparent colors
          clipImage.setRGB(column, row, iconBackground.getRGB());
      }

    try                           // clipboard may not be available
    {
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
        (Transferable) this, null); // place data notice on clipboard
    }
    catch (IllegalStateException ise)
    {
      JOptionPane.showMessageDialog(IconEdit1.mainFrame,
        ("Can't put image on clipboard:\n" + ise.getMessage()));
    }
  } // end of copyThisIcon() method

  /* The following methods are part of the Transferable interface called by
  copyThisIcon(). */

  public Object getTransferData(DataFlavor flavor)
    throws IOException, UnsupportedFlavorException
  {
    if (clipImage == null)
      throw new IOException("no clipboard image created");
    else if (flavor.equals(DataFlavor.imageFlavor))
      return(clipImage);
    else
      throw new UnsupportedFlavorException(flavor);
  }

  public DataFlavor[] getTransferDataFlavors()
  {
    final DataFlavor[] result = { DataFlavor.imageFlavor };
    return(result);
  }

  public boolean isDataFlavorSupported(DataFlavor flavor)
  {
    return(flavor.equals(DataFlavor.imageFlavor));
  }

  /* Paste an image from the clipboard to this icon.  The Java 1.4 clipboard
  doesn't retain transparency information, so all pixels become solid colors
  (opaque). */

  void pasteThisIcon()
  {
    int column, row;              // zero-based indices into icon data
    int newHeight, newWidth;      // size of received image in pixels
    BufferedImage newImage;       // image received from clipboard

    /* First try to get an image (any image!) from the clipboard. */

    try
    {
      newImage = (BufferedImage) Toolkit.getDefaultToolkit()
        .getSystemClipboard().getContents(null)
        .getTransferData(DataFlavor.imageFlavor);
    }
    catch (IllegalStateException ise) { newImage = null; }
    catch (IOException ioe) { newImage = null; }
    catch (UnsupportedFlavorException ufe) { newImage = null; }
    if (newImage == null)        // quit if we couldn't find an image
    {
      IconEdit1.statusDialog.setText("Sorry, no image found on clipboard.");
      return;                     // do nothing more
    }

    /* Check the size of the clipboard image.  We don't crop or resize.  Yes,
    there is a real character for the "times" sign (x), but I prefer to use
    plain text characters in messages. */

    newHeight = newImage.getHeight(); // get image's size in pixels
    newWidth = newImage.getWidth();
    if ((newHeight != size) || (newWidth != size))
    {
      IconEdit1.statusDialog.setText("Sorry, image is " + newWidth + " x "
        + newHeight + " and icon is " + size + " x " + size
        + ".  Please resize.");
      return;                     // do nothing more
    }

    /* If we received an image that we previously put on the clipboard, then
    use our saved internal data instead of the clipboard image, because the
    clipboard loses color depth and transparency information. */

    clearThisIcon();              // reset this icon to all default values

    if (clipImage == newImage)    // is clipboard still holding our image?
    {
      for (row = 0; row < size; row ++)
        for (column = 0; column < size; column ++)
        {
          colors[row][column] = clipColors[row][column]; // copy old colors
          isOpaque[row][column] = clipOpaque[row][column]; // copy transparency
        }

      colorDepth = clipDepth;     // get old color depth from internal data
      if (colorDepth == 4)        // 4-bit color (16 colors)?
        depthBit4.setSelected(true); // change radio button to match
      else if (colorDepth == 8)   // 8-bit color (256 colors)?
        depthBit8.setSelected(true); // change radio button to match
      else                        // assume 24-bit RGB color
        depthBit24.setSelected(true); // change radio button to match
    }

    /* The clipboard has an image from somewhere else.  Copy the image pixels
    to our icon.  With plain Image objects, this is quite awkward, so be happy
    that we can use a BufferedImage! */

    else                          // clipboard image is not from us originally
    {
      clipColors = null;          // cancel saved color information (if any)
      clipImage = null;           // cancel saved buffered image (if any)
      clipOpaque = null;          // cancel saved transparency flags (if any)

      for (row = 0; row < size; row ++)
        for (column = 0; column < size; column ++)
        {
          colors[row][column] = new Color(newImage.getRGB(column, row));
          isOpaque[row][column] = true; // force pixel to have solid color
        }

      /* Estimate the color depth from the RGB values used by the pixels.  We
      have no way of knowing what the color depth was in the application that
      provided this image. */

      if (IconEdit1.colorsAreWindows16(colors)) // 16-color standard?
      {
        colorDepth = 4;           // assume 4-bit color depth from clipboard
        depthBit4.setSelected(true); // change radio button to match
      }
      else if (IconEdit1.colorsAreWebSafe(colors)) // 256-color standard?
      {
        colorDepth = 8;           // assume 8-bit color depth from clipboard
        depthBit8.setSelected(true); // change radio button to match
      }
      else                        // colors are not from a known palette
      {
        colorDepth = 24;          // assume 24-bit color depth from clipboard
        depthBit24.setSelected(true); // change radio button to match
      }
    }

    /* Repaint the icon panel, no matter where we got the image from (clipboard
    or internal). */

    this.repaint();               // and finally display new icon samples

  } // end of pasteThisIcon() method

  /* Sliders fire a change event instead of an action event. */

  public void stateChanged(ChangeEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == backSlider)     // change background for sample icons
    {
      iconBackground = backColors[backSlider.getValue()]; // new background
      this.repaint();             // and redisplay icon samples
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in stateChanged(): unknown ChangeEvent: "
        + event);                 // should never happen, so write on console
    }
  }

} // end of IconEdit1Icon class

// ------------------------------------------------------------------------- //

/*
  IconEdit1User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class IconEdit1User extends AbstractAction implements ChangeListener
{
  /* empty constructor */

  public IconEdit1User() { }

  /* button listener, dialog boxes, keyboard, etc */

  public void actionPerformed(ActionEvent event)
  {
    IconEdit1.userButton(event);
  }

  /* changes to color selection dialog */

  public void stateChanged(ChangeEvent event)
  {
    IconEdit1.getSelectedColor();
  }

} // end of IconEdit1User class

/* Copyright (c) 2007 by Keith Fenske.  Apache License or GNU GPL. */
