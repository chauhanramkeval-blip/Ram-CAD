package com.example.ui.screens.editor.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

/**
 * Central registry of all CAD tools, categories, commands, and availability metadata.
 * Ensures the UI is decoupled from the CAD engine and driven declaratively by ToolDefinitions.
 */
object ToolRegistry {

    private val allTools: List<ToolDefinition> = listOf(
        // ==========================================
        // 1. ANNOTATION POPUP TOOLS
        // ==========================================
        ToolDefinition(
            id = "annot_text",
            name = "Text",
            icon = Icons.Filled.TextFields,
            category = PopupCategory.ANNOTATION,
            description = "Single-line drafting text annotation",
            command = "CMD_ANNOT_TEXT",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "annot_mtext",
            name = "Multiline Text",
            icon = Icons.Filled.Notes,
            category = PopupCategory.ANNOTATION,
            description = "Multi-line paragraph text note block",
            command = "CMD_ANNOT_MTEXT",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "annot_leader",
            name = "Leader",
            icon = Icons.Filled.CallMade,
            category = PopupCategory.ANNOTATION,
            description = "Pointed callout leader note with landing line",
            command = "CMD_ANNOT_LEADER",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "annot_multileader",
            name = "Multileader",
            icon = Icons.Filled.Share,
            category = PopupCategory.ANNOTATION,
            description = "Composite callout arrow pointing to multiple locations",
            command = "CMD_ANNOT_MULTILEADER",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "annot_arrow",
            name = "Arrow",
            icon = Icons.Filled.East,
            category = PopupCategory.ANNOTATION,
            description = "Directional callout arrow annotation",
            command = "CMD_ANNOT_ARROW",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "annot_cloud",
            name = "Revision Cloud",
            icon = Icons.Filled.Cloud,
            category = PopupCategory.ANNOTATION,
            description = "Arc-segmented cloud highlight for engineering revisions",
            command = "CMD_ANNOT_CLOUD",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "annot_markup",
            name = "Markup",
            icon = Icons.Filled.EditNote,
            category = PopupCategory.ANNOTATION,
            description = "Freehand stylus markup & cloud review comments",
            command = "CMD_ANNOT_MARKUP",
            availability = ToolAvailability.COMING_SOON
        ),

        // ==========================================
        // 2. DRAW POPUP TOOLS
        // ==========================================
        ToolDefinition(
            id = "draw_polyline",
            name = "Polyline",
            icon = Icons.Filled.Timeline,
            category = PopupCategory.DRAW,
            description = "Continuous 2D multi-segment polyline path",
            command = "CMD_DRAW_POLYLINE",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "draw_line",
            name = "Line",
            icon = Icons.Filled.ShowChart,
            category = PopupCategory.DRAW,
            description = "2D straight line segment between two coordinates",
            command = "CMD_DRAW_LINE",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "draw_text",
            name = "Text",
            icon = Icons.Filled.TextFields,
            category = PopupCategory.DRAW,
            description = "Place vector drafting text on canvas",
            command = "CMD_DRAW_TEXT",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "draw_circle",
            name = "Circle",
            icon = Icons.Filled.Circle,
            category = PopupCategory.DRAW,
            description = "Center and radius / diameter circle entity",
            command = "CMD_DRAW_CIRCLE",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "draw_arc",
            name = "Arc",
            icon = Icons.Filled.LinearScale,
            category = PopupCategory.DRAW,
            description = "3-point circular arc curve segment",
            command = "CMD_DRAW_ARC",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "draw_rectangle",
            name = "Rectangle",
            icon = Icons.Filled.CropSquare,
            category = PopupCategory.DRAW,
            description = "Orthogonal 4-sided closed polyline rectangle",
            command = "CMD_DRAW_RECTANGLE",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "draw_ellipse",
            name = "Ellipse",
            icon = Icons.Filled.PanoramaFishEye,
            category = PopupCategory.DRAW,
            description = "Parametric major/minor axis ellipse requires 2D conic solver",
            command = "CMD_DRAW_ELLIPSE",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "draw_sketch",
            name = "Sketch",
            icon = Icons.Filled.Gesture,
            category = PopupCategory.DRAW,
            description = "Freehand point stream sampling requires bezier curve-fitting engine",
            command = "CMD_DRAW_SKETCH",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "draw_smart_pen",
            name = "Smart Pen",
            icon = Icons.Filled.AutoFixHigh,
            category = PopupCategory.DRAW,
            description = "Heuristic shape recognition requires spatial geometry engine",
            command = "CMD_DRAW_SMART_PEN",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "draw_multileader",
            name = "Multileader",
            icon = Icons.Filled.CallMade,
            category = PopupCategory.DRAW,
            description = "Drafting callout leader note with pointing arrowhead",
            command = "CMD_DRAW_MULTILEADER",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "draw_cloud",
            name = "Revision Cloud",
            icon = Icons.Filled.Cloud,
            category = PopupCategory.DRAW,
            description = "Scalloped revision cloud perimeter boundary",
            command = "CMD_DRAW_CLOUD",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "draw_divide",
            name = "Divide",
            icon = Icons.Filled.Splitscreen,
            category = PopupCategory.DRAW,
            description = "Parametric division points along curve require curve-subdivision engine",
            command = "CMD_DRAW_DIVIDE",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),

        // ==========================================
        // 3. EDIT POPUP TOOLS
        // ==========================================
        ToolDefinition(
            id = "edit_select",
            name = "Select",
            icon = Icons.Filled.NearMe,
            category = PopupCategory.EDIT,
            description = "Selection mode: tap or drag window/crossing box",
            command = "CMD_EDIT_SELECT",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "edit_move",
            name = "Move",
            icon = Icons.Filled.OpenWith,
            category = PopupCategory.EDIT,
            description = "Translate selected objects by base point and displacement",
            command = "CMD_EDIT_MOVE",
            availability = ToolAvailability.AVAILABLE,
            requiresSelection = true
        ),
        ToolDefinition(
            id = "edit_copy",
            name = "Copy",
            icon = Icons.Filled.ContentCopy,
            category = PopupCategory.EDIT,
            description = "Duplicate selected objects at specified displacement",
            command = "CMD_EDIT_COPY",
            availability = ToolAvailability.AVAILABLE,
            requiresSelection = true
        ),
        ToolDefinition(
            id = "edit_rotate",
            name = "Rotate",
            icon = Icons.Filled.RotateRight,
            category = PopupCategory.EDIT,
            description = "Rotate selected entities about a pivot point",
            command = "CMD_EDIT_ROTATE",
            availability = ToolAvailability.AVAILABLE,
            requiresSelection = true
        ),
        ToolDefinition(
            id = "edit_scale",
            name = "Scale",
            icon = Icons.Filled.ZoomOutMap,
            category = PopupCategory.EDIT,
            description = "Resize selected entities about base point by scale factor",
            command = "CMD_EDIT_SCALE",
            availability = ToolAvailability.AVAILABLE,
            requiresSelection = true
        ),
        ToolDefinition(
            id = "edit_mirror",
            name = "Mirror",
            icon = Icons.Filled.Flip,
            category = PopupCategory.EDIT,
            description = "Reflect entities across an axis requires 2D matrix mirror solver",
            command = "CMD_EDIT_MIRROR",
            availability = ToolAvailability.ENGINE_REQUIRED,
            requiresSelection = true
        ),
        ToolDefinition(
            id = "edit_trim",
            name = "Trim",
            icon = Icons.Filled.ContentCut,
            category = PopupCategory.EDIT,
            description = "Trim entity segments at cutting edge intersections",
            command = "CMD_EDIT_TRIM",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "edit_extend",
            name = "Extend",
            icon = Icons.Filled.East,
            category = PopupCategory.EDIT,
            description = "Extend entity endpoints to meet boundary edges",
            command = "CMD_EDIT_EXTEND",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "edit_offset",
            name = "Offset",
            icon = Icons.Filled.FilterNone,
            category = PopupCategory.EDIT,
            description = "Concentric parallel curve offset requires spatial topology engine",
            command = "CMD_EDIT_OFFSET",
            availability = ToolAvailability.ENGINE_REQUIRED,
            requiresSelection = true
        ),
        ToolDefinition(
            id = "edit_fillet",
            name = "Fillet",
            icon = Icons.Filled.RoundedCorner,
            category = PopupCategory.EDIT,
            description = "Arc fillet rounding between curves requires bi-tangent arc solver",
            command = "CMD_EDIT_FILLET",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "edit_chamfer",
            name = "Chamfer",
            icon = Icons.Filled.BorderStyle,
            category = PopupCategory.EDIT,
            description = "Beveled angled corner cut requires geometric chamfer solver",
            command = "CMD_EDIT_CHAMFER",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "edit_erase",
            name = "Erase",
            icon = Icons.Filled.Delete,
            category = PopupCategory.EDIT,
            description = "Delete selected entities or tap objects to erase",
            command = "CMD_EDIT_ERASE",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "edit_explode",
            name = "Explode",
            icon = Icons.Filled.Grain,
            category = PopupCategory.EDIT,
            description = "Break complex blocks/polylines requires block decomposition engine",
            command = "CMD_EDIT_EXPLODE",
            availability = ToolAvailability.ENGINE_REQUIRED,
            requiresSelection = true
        ),
        ToolDefinition(
            id = "edit_join",
            name = "Join",
            icon = Icons.Filled.Link,
            category = PopupCategory.EDIT,
            description = "Weld coincident segments into polylines requires vertex merge engine",
            command = "CMD_EDIT_JOIN",
            availability = ToolAvailability.ENGINE_REQUIRED,
            requiresSelection = true
        ),

        // ==========================================
        // 4. LAYER POPUP TOOLS
        // ==========================================
        ToolDefinition(
            id = "layer_list",
            name = "Layer List",
            icon = Icons.Filled.Layers,
            category = PopupCategory.LAYER,
            description = "Open full CAD layer manager with search and filters",
            command = "CMD_LAYER_LIST",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "layer_new",
            name = "New Layer",
            icon = Icons.Filled.Add,
            category = PopupCategory.LAYER,
            description = "Create a new drafting layer with custom color and properties",
            command = "CMD_LAYER_NEW",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "layer_delete",
            name = "Delete Layer",
            icon = Icons.Filled.DeleteOutline,
            category = PopupCategory.LAYER,
            description = "Delete current non-default layer from document",
            command = "CMD_LAYER_DELETE",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "layer_current",
            name = "Current Layer",
            icon = Icons.Filled.CheckCircle,
            category = PopupCategory.LAYER,
            description = "Cycle or switch the active drawing layer",
            command = "CMD_LAYER_CURRENT",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "layer_visibility",
            name = "Layer Visibility",
            icon = Icons.Filled.Visibility,
            category = PopupCategory.LAYER,
            description = "Toggle visibility on/off for current active layer",
            command = "CMD_LAYER_VISIBILITY",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "layer_freeze",
            name = "Freeze/Thaw",
            icon = Icons.Filled.AcUnit,
            category = PopupCategory.LAYER,
            description = "Viewport layer freeze requires viewport table engine",
            command = "CMD_LAYER_FREEZE",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "layer_lock",
            name = "Lock/Unlock",
            icon = Icons.Filled.Lock,
            category = PopupCategory.LAYER,
            description = "Lock active layer to protect entities from modification",
            command = "CMD_LAYER_LOCK",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "layer_color",
            name = "Layer Color",
            icon = Icons.Filled.Palette,
            category = PopupCategory.LAYER,
            description = "Change display color of active drafting layer",
            command = "CMD_LAYER_COLOR",
            availability = ToolAvailability.AVAILABLE
        ),

        // ==========================================
        // 5. MEASURE POPUP TOOLS
        // ==========================================
        ToolDefinition(
            id = "measure_distance",
            name = "Distance",
            icon = Icons.Filled.Straighten,
            category = PopupCategory.MEASURE,
            description = "Measure true 2D Euclidean distance between two points",
            command = "CMD_MEASURE_DISTANCE",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "measure_continuous",
            name = "Continuous",
            icon = Icons.Filled.Timeline,
            category = PopupCategory.MEASURE,
            description = "Continuous multi-point polyline distance and segment breakdown",
            command = "CMD_MEASURE_CONTINUOUS",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "measure_batch",
            name = "Batch",
            icon = Icons.Filled.SelectAll,
            category = PopupCategory.MEASURE,
            description = "Batch calculate total perimeter and area of selected entities",
            command = "CMD_MEASURE_BATCH",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "measure_area",
            name = "Area",
            icon = Icons.Filled.SquareFoot,
            category = PopupCategory.MEASURE,
            description = "Compute closed polygon area and perimeter using Green's Theorem",
            command = "CMD_MEASURE_AREA",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "measure_facade",
            name = "Facade",
            icon = Icons.Filled.HomeWork,
            category = PopupCategory.MEASURE,
            description = "Vertical elevation projection requires spatial projection engine",
            command = "CMD_MEASURE_FACADE",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "measure_id_point",
            name = "ID Point",
            icon = Icons.Filled.CenterFocusStrong,
            category = PopupCategory.MEASURE,
            description = "Inspect absolute world coordinates (X, Y) of tapped location",
            command = "CMD_MEASURE_ID_POINT",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "measure_arc_length",
            name = "Arc Length",
            icon = Icons.Filled.LinearScale,
            category = PopupCategory.MEASURE,
            description = "Calculate perimeter and arc curve distance along path",
            command = "CMD_MEASURE_ARC_LENGTH",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "measure_entity",
            name = "Entity",
            icon = Icons.Filled.TouchApp,
            category = PopupCategory.MEASURE,
            description = "Inspect geometric properties and dimensions of tapped entity",
            command = "CMD_MEASURE_ENTITY",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "measure_angle",
            name = "Angle",
            icon = Icons.Filled.Architecture,
            category = PopupCategory.MEASURE,
            description = "Measure interior angle formed by vertex and two ray endpoints",
            command = "CMD_MEASURE_ANGLE",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "measure_scale",
            name = "Scale",
            icon = Icons.Filled.Speed,
            category = PopupCategory.MEASURE,
            description = "Calibrated viewport scaling requires dynamic calibration engine",
            command = "CMD_MEASURE_SCALE",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "measure_result",
            name = "Result",
            icon = Icons.Filled.Calculate,
            category = PopupCategory.MEASURE,
            description = "Open complete measurement results, multi-area regions & units sheet",
            command = "CMD_MEASURE_RESULT",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "measure_result_count",
            name = "Result Count",
            icon = Icons.Filled.History,
            category = PopupCategory.MEASURE,
            description = "View recorded measurement session history and recall entries",
            command = "CMD_MEASURE_RESULT_COUNT",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "measure_precision",
            name = "Precision",
            icon = Icons.Filled.Tune,
            category = PopupCategory.MEASURE,
            description = "Cycle precision unit formatting (mm, cm, m, in, ft)",
            command = "CMD_MEASURE_PRECISION",
            availability = ToolAvailability.AVAILABLE
        ),

        // ==========================================
        // 6. DIMENSION POPUP TOOLS
        // ==========================================
        ToolDefinition(
            id = "dim_linear",
            name = "Linear",
            icon = Icons.Filled.SquareFoot,
            category = PopupCategory.DIMENSION,
            description = "Linear horizontal or vertical dimension line",
            command = "CMD_DIM_LINEAR",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "dim_aligned",
            name = "Aligned",
            icon = Icons.Filled.Straighten,
            category = PopupCategory.DIMENSION,
            description = "Aligned dimension parallel to extension points",
            command = "CMD_DIM_ALIGNED",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "dim_angular",
            name = "Angular",
            icon = Icons.Filled.Architecture,
            category = PopupCategory.DIMENSION,
            description = "Angular dimension arc between two lines",
            command = "CMD_DIM_ANGULAR",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "dim_radius",
            name = "Radius",
            icon = Icons.Filled.RadioButtonChecked,
            category = PopupCategory.DIMENSION,
            description = "Radial dimension callout with R prefix",
            command = "CMD_DIM_RADIUS",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "dim_diameter",
            name = "Diameter",
            icon = Icons.Filled.Adjust,
            category = PopupCategory.DIMENSION,
            description = "Diametric dimension line across circle with Ø prefix",
            command = "CMD_DIM_DIAMETER",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "dim_arc_length",
            name = "Arc Length",
            icon = Icons.Filled.LinearScale,
            category = PopupCategory.DIMENSION,
            description = "Curved arc length dimension requires dimension arc renderer",
            command = "CMD_DIM_ARC_LENGTH",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "dim_ordinate",
            name = "Ordinate",
            icon = Icons.Filled.LocationSearching,
            category = PopupCategory.DIMENSION,
            description = "Datum ordinate dimension requires datum coordinate engine",
            command = "CMD_DIM_ORDINATE",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "dim_continuous",
            name = "Continuous",
            icon = Icons.Filled.FormatLineSpacing,
            category = PopupCategory.DIMENSION,
            description = "Chained continuous dimensioning requires associative dimensioner",
            command = "CMD_DIM_CONTINUOUS",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "dim_baseline",
            name = "Baseline",
            icon = Icons.Filled.VerticalAlignBottom,
            category = PopupCategory.DIMENSION,
            description = "Stacked baseline offset dimension requires baseline dimension engine",
            command = "CMD_DIM_BASELINE",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),

        // ==========================================
        // 7. COLOR POPUP TOOLS
        // ==========================================
        ToolDefinition(
            id = "color_bylayer",
            name = "ByLayer",
            icon = Icons.Filled.Layers,
            category = PopupCategory.COLOR,
            description = "Inherit entity color dynamically from assigned layer",
            command = "CMD_COLOR_BYLAYER",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "color_byblock",
            name = "ByBlock",
            icon = Icons.Filled.ViewInAr,
            category = PopupCategory.COLOR,
            description = "Inherit color from parent insertion block",
            command = "CMD_COLOR_BYBLOCK",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "color_object",
            name = "Object Color",
            icon = Icons.Filled.ColorLens,
            category = PopupCategory.COLOR,
            description = "Assign explicit standard CAD color (Red, Cyan, Green, Yellow, White)",
            command = "CMD_COLOR_OBJECT",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "color_custom",
            name = "Custom Color",
            icon = Icons.Filled.Palette,
            category = PopupCategory.COLOR,
            description = "Choose custom true color 24-bit RGB value",
            command = "CMD_COLOR_CUSTOM",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "color_transparency",
            name = "Transparency",
            icon = Icons.Filled.InvertColors,
            category = PopupCategory.COLOR,
            description = "Alpha blending transparency requires hardware compositor engine",
            command = "CMD_COLOR_TRANSPARENCY",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),

        // ==========================================
        // 8. TOOL POPUP TOOLS
        // ==========================================
        ToolDefinition(
            id = "tool_osnap",
            name = "OSNAP",
            icon = Icons.Filled.CenterFocusStrong,
            category = PopupCategory.TOOL,
            description = "Object Snap modes (Endpoint, Midpoint, Center, Intersection)",
            command = "CMD_TOOL_OSNAP",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "tool_grid",
            name = "Grid",
            icon = Icons.Filled.GridOn,
            category = PopupCategory.TOOL,
            description = "Toggle engineering background reference grid",
            command = "CMD_TOOL_GRID",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "tool_ortho",
            name = "Ortho",
            icon = Icons.Filled.BorderOuter,
            category = PopupCategory.TOOL,
            description = "Restrict cursor movement to 90° horizontal/vertical axes",
            command = "CMD_TOOL_ORTHO",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "tool_polar",
            name = "Polar Tracking",
            icon = Icons.Filled.Navigation,
            category = PopupCategory.TOOL,
            description = "Incremental polar angle guide rays require polar ray engine",
            command = "CMD_TOOL_POLAR",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "tool_snap",
            name = "Snap",
            icon = Icons.Filled.MyLocation,
            category = PopupCategory.TOOL,
            description = "Toggle snapping cursor to defined grid intervals",
            command = "CMD_TOOL_SNAP",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "tool_coordinates",
            name = "Coordinates",
            icon = Icons.Filled.Explore,
            category = PopupCategory.TOOL,
            description = "Toggle dynamic drafting crosshairs and coordinate HUD",
            command = "CMD_TOOL_COORDINATES",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "tool_units",
            name = "Units",
            icon = Icons.Filled.Straighten,
            category = PopupCategory.TOOL,
            description = "Switch drawing units (Millimeters, Centimeters, Meters, Inches, Feet)",
            command = "CMD_TOOL_UNITS",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "tool_precision",
            name = "Precision",
            icon = Icons.Filled.Tune,
            category = PopupCategory.TOOL,
            description = "Cycle precision decimal display (0, 0.0, 0.00, 0.000)",
            command = "CMD_TOOL_PRECISION",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "tool_properties",
            name = "Properties",
            icon = Icons.Filled.Settings,
            category = PopupCategory.TOOL,
            description = "Inspect and edit selected entity attributes & geometry",
            command = "CMD_TOOL_PROPERTIES",
            availability = ToolAvailability.AVAILABLE
        ),

        // ==========================================
        // 9. LAYOUT POPUP TOOLS
        // ==========================================
        ToolDefinition(
            id = "layout_model",
            name = "Model",
            icon = Icons.Filled.ViewInAr,
            category = PopupCategory.LAYOUT,
            description = "Active Model Space infinite drafting environment",
            command = "CMD_LAYOUT_MODEL",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "layout_1",
            name = "Layout 1",
            icon = Icons.Filled.Tab,
            category = PopupCategory.LAYOUT,
            description = "Paper Space Sheet 1 requires paper viewport layout engine",
            command = "CMD_LAYOUT_1",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "layout_2",
            name = "Layout 2",
            icon = Icons.Filled.Tab,
            category = PopupCategory.LAYOUT,
            description = "Paper Space Sheet 2 requires paper viewport layout engine",
            command = "CMD_LAYOUT_2",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "layout_new",
            name = "New Layout",
            icon = Icons.Filled.AddBox,
            category = PopupCategory.LAYOUT,
            description = "Create paper sheet requires multi-sheet layout engine",
            command = "CMD_LAYOUT_NEW",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "layout_delete",
            name = "Delete Layout",
            icon = Icons.Filled.IndeterminateCheckBox,
            category = PopupCategory.LAYOUT,
            description = "Delete paper sheet requires sheet table manager",
            command = "CMD_LAYOUT_DELETE",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "layout_page_setup",
            name = "Page Setup",
            icon = Icons.Filled.Print,
            category = PopupCategory.LAYOUT,
            description = "Plot style & sheet margins (A1/A2/A3/A4) require print engine",
            command = "CMD_LAYOUT_PAGE_SETUP",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "layout_viewport",
            name = "Viewport",
            icon = Icons.Filled.PictureInPicture,
            category = PopupCategory.LAYOUT,
            description = "Model viewport window requires clipping viewport engine",
            command = "CMD_LAYOUT_VIEWPORT",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),

        // ==========================================
        // 10. VISUAL STYLE POPUP TOOLS
        // ==========================================
        ToolDefinition(
            id = "vs_2d_wireframe",
            name = "2D Wireframe",
            icon = Icons.Filled.Architecture,
            category = PopupCategory.VISUAL_STYLE,
            description = "Standard 2D high-performance vector wireframe drafting",
            command = "CMD_VS_WIREFRAME",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "vs_hidden",
            name = "Hidden",
            icon = Icons.Filled.BlurOn,
            category = PopupCategory.VISUAL_STYLE,
            description = "Hidden line removal rendering requires 3D facet occluder engine",
            command = "CMD_VS_HIDDEN",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "vs_shaded",
            name = "Shaded",
            icon = Icons.Filled.Category,
            category = PopupCategory.VISUAL_STYLE,
            description = "Smooth Gouraud/Phong surface shading requires 3D mesh rasterizer",
            command = "CMD_VS_SHADED",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "vs_shaded_edges",
            name = "Shaded with Edges",
            icon = Icons.Filled.BorderAll,
            category = PopupCategory.VISUAL_STYLE,
            description = "Shaded facets with wireframe perimeter requires composite shader",
            command = "CMD_VS_SHADED_EDGES",
            availability = ToolAvailability.ENGINE_REQUIRED
        ),
        ToolDefinition(
            id = "vs_lineweight",
            name = "Lineweight",
            icon = Icons.Filled.LineWeight,
            category = PopupCategory.VISUAL_STYLE,
            description = "Toggle visual display of technical pen line weights",
            command = "CMD_VS_LINEWEIGHT",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "vs_background",
            name = "Background",
            icon = Icons.Filled.Contrast,
            category = PopupCategory.VISUAL_STYLE,
            description = "Cycle canvas background (Black, Charcoal, Navy, Blueprint)",
            command = "CMD_VS_BACKGROUND",
            availability = ToolAvailability.AVAILABLE
        ),
        ToolDefinition(
            id = "vs_grid",
            name = "Grid",
            icon = Icons.Filled.GridOn,
            category = PopupCategory.VISUAL_STYLE,
            description = "Toggle visual drafting grid coordinate overlay",
            command = "CMD_VS_GRID",
            availability = ToolAvailability.AVAILABLE
        )
    )

    /**
     * Retrieve all registered tools.
     */
    fun getAllTools(): List<ToolDefinition> = allTools

    /**
     * Retrieve all tools belonging to a specific popup category.
     */
    fun getToolsByCategory(category: PopupCategory): List<ToolDefinition> {
        return allTools.filter { it.popupCategory == category }
    }

    /**
     * Find a tool by its unique ID.
     */
    fun getToolById(id: String): ToolDefinition? {
        return allTools.find { it.id == id }
    }

    /**
     * Retrieve the list of all 10 popup categories in the canonical toolbar sequence.
     */
    val categories: List<PopupCategory> = listOf(
        PopupCategory.ANNOTATION,
        PopupCategory.DRAW,
        PopupCategory.EDIT,
        PopupCategory.LAYER,
        PopupCategory.MEASURE,
        PopupCategory.DIMENSION,
        PopupCategory.COLOR,
        PopupCategory.TOOL,
        PopupCategory.LAYOUT,
        PopupCategory.VISUAL_STYLE
    )

    /**
     * Returns whether all tools in a category are implemented or require external engine.
     */
    fun getAvailabilitySummary(): Map<ToolAvailability, Int> {
        return allTools.groupingBy { it.availability }.eachCount()
    }
}
