package com.bigchaindb.smartchaindb.driver;

import java.util.HashSet;

public interface Capabilities {

    String PLASTIC = "Plastics_Manufacturing";
    String PRINTING_3D = "3D_Printing";
    String POCKET_MACHINING = "Pocket_machining";
    String MILLING = "Milling";
    String TURNING = "Turning";
    String DRILLING = "Drilling";
    String THREADING = "Threading";
    String BORING = "Boring";
    String GROOVING = "Grooving";
    String MISC = "Miscellaneous";


    String FEEDING = "Feeding";
    String MANIPULATION = "Manipulation";
    // Joining Manfacturing Process starts here
    String ADHESIVE_CURING = "AdhesiveCuring";
    String SOLDERING = "Soldering";
    String WELDING = "Welding";
    String FASTENING = "Fastening";
    String COLD_WELDING = "ColdWelding";
    String PLASTIC_WELDING = "PlasticWelding";
    String ULTRASONIC_WELDING = "UltrasonicWelding";
    String HEATED_AIR_WELDING = "HeatedAirWelding";
    String FRICTION_WELDING = "FrictionWelding";
    String METAL_WELDING = "MetalWelding";
    String ARC_WELDING = "ArcWelding";
    String RESISTANCE_WELDING = "ResistanceWelding";
    String LASER_WELDING = "LaserWelding";
    String CLAMPING = "Clamping";
    String SCREWING = "Screwing";
    String NAILING = "Nailing";
    String SEWING = "Sewing";
    String TAPING = "Taping";
    String STAPLING = "Stapling";
    String THREAD_RUNNING = "ThreadRunning";
    String RIVETING = "Riveting";
    String SPRING_FASTENING = "SpringFastening";
    String TIGHTENING_FROM_SIDE = "TighteningFromSide";
    String DEFORMING_FIXATING = "DeformingFixating";
    String ELASTIC_DEFORMING_FIXATING = "ElasticDeformingFixating";
    String PLASTIC_DEFORMING_FIXATING = "PlasticDeformingFixating";
    // Preparation starts here
    String SOLDER_PASTE_APPLICATION = "SolderPasteApplication";
    String UNPACKING = "Unpacking";
    String LOADING = "Loading";
    String FIXTURING = "Fixturing";
    String ADHESIVE_APPLICATION = "AdhesiveApplication";
    // Finalization starts here
    String MARKING = "Marking";
    String PACKAGING = "Packaging";
    String SURFACE_FINISHING = "SurfaceFinishing";
    String UNLOADING = "Unloading";
    String CLEANING = "Cleaning";
    // Shaping Manufacturing starts here
    String MATERIAL_CONSERVING = "MaterialConserving";
    String MATERIAL_REMOVING = "MaterialRemoving";
    String MATERIAL_ADDING = "MaterialAdding";
    String MACHINING = "Machining";
    String LASER_CUTTING = "LaserCutting";
    String PUNCHING = "Punching";
    String GRINDING = "Grinding";
    String MOULDING = "Moulding";
    String CASTING = "Casting";
    String LASER_ADDING = "LaserAdding";
    String FORMING = "Forming";
    // Non-shaping Manufacturing starts here
    String HEAT_TREATMENT = "HeatTreatment";
    String LOGISTIC = "Logistic";
    String QUALIFYING = "Qualifying";

    String TORCH_SOLDERING = "TorchSoldering";
    String IRON_SOLDERING = "IronSoldering";
    String DIP_SOLDERING = "DipSoldering";
    String THERMAL_CURING = "ThermalCuring";
    String UV_CURING = "UV-curing";
    String GLUING = "Gluing";
    String WEDGING = "Wedging";
    String CLIPPING = "Clipping";
    String PRESSING = "Pressing";
    String SHRINKING = "Shrinking";
    String FOLDING = "Folding";
    String TWISTING = "Twisting";
    String BENDING = "Bending";
    String HAMMERING = "Hammering";
    String MATING = "Mating";
    String ORIENTING = "Orienting";
    String FLIPPING = "Flipping";
    String HOLDING = "Holding";
    String PICKING = "Picking";
    String POSITIONING = "Positioning";
    String GRASPING = "Grasping";
    String PLACING = "Placing";
    String BULK_FEEDING = "BulkFeeding";
    String TRAY_FEEDING = "TrayFeeding";
    String TUBE_FEEDING = "TubeFeeding";
    String TAPE_FEEDING = "TapeFeeding";

    static HashSet<String> getAll() {
        final HashSet<String> cap = new HashSet<>();
        cap.add(Capabilities.FEEDING);
        cap.add(Capabilities.MANIPULATION);
        cap.add(Capabilities.FASTENING);
        cap.add(Capabilities.WELDING);
        cap.add(Capabilities.SOLDERING);
        cap.add(Capabilities.ADHESIVE_CURING);
        cap.add(Capabilities.DEFORMING_FIXATING);
        cap.add(Capabilities.MACHINING);
        cap.add(Capabilities.MOULDING);
        cap.add(Capabilities.CASTING);

        return cap;
    }

    static HashSet<String> getAllRequestTopics() {
        final HashSet<String> cap = new HashSet<>();
        cap.add(Capabilities.SCREWING);
        cap.add(Capabilities.CLAMPING);
        cap.add(Capabilities.TAPING);
        cap.add(Capabilities.SEWING);
        cap.add(Capabilities.NAILING);
        cap.add(Capabilities.RIVETING);
        cap.add(Capabilities.STAPLING);
        cap.add(Capabilities.SPRING_FASTENING);
        cap.add(Capabilities.THREAD_RUNNING);
        cap.add(Capabilities.TIGHTENING_FROM_SIDE);
        cap.add(Capabilities.COLD_WELDING);
        cap.add(Capabilities.PLASTIC_WELDING);
        cap.add(Capabilities.ULTRASONIC_WELDING);
        cap.add(Capabilities.HEATED_AIR_WELDING);
        cap.add(Capabilities.FRICTION_WELDING);
		// 15
        cap.add(Capabilities.METAL_WELDING);
        cap.add(Capabilities.ARC_WELDING);
        cap.add(Capabilities.RESISTANCE_WELDING);
        cap.add(Capabilities.LASER_WELDING);
        cap.add(Capabilities.TORCH_SOLDERING);
        cap.add(Capabilities.IRON_SOLDERING);
        cap.add(Capabilities.DIP_SOLDERING);
        cap.add(Capabilities.THERMAL_CURING);
        cap.add(Capabilities.UV_CURING);
        cap.add(Capabilities.GLUING);
        cap.add(Capabilities.WEDGING);
        cap.add(Capabilities.CLIPPING);
        cap.add(Capabilities.PRESSING);
        cap.add(Capabilities.SHRINKING);
        cap.add(Capabilities.FOLDING);
		// 30
        // cap.add(Capabilities.TWISTING);
        // cap.add(Capabilities.BENDING);
        // cap.add(Capabilities.HAMMERING);
        // cap.add(Capabilities.MATING);
        // cap.add(Capabilities.ORIENTING);
        // cap.add(Capabilities.FLIPPING);
        // cap.add(Capabilities.HOLDING);
        // cap.add(Capabilities.PICKING);
        // cap.add(Capabilities.POSITIONING);
        // cap.add(Capabilities.GRASPING);
        // cap.add(Capabilities.PLACING);
        // cap.add(Capabilities.BULK_FEEDING);
        // cap.add(Capabilities.TRAY_FEEDING);
        // cap.add(Capabilities.TUBE_FEEDING);
        // cap.add(Capabilities.TAPE_FEEDING);
		// // 45
        // cap.add(Capabilities.TURNING);
        // cap.add(Capabilities.MILLING);
        // cap.add(Capabilities.DRILLING);
        // cap.add(Capabilities.THREADING);
        // cap.add(Capabilities.BORING);
        // cap.add(Capabilities.GROOVING);
        // cap.add(Capabilities.MOULDING);
        // cap.add(Capabilities.CASTING);
        // cap.add(Capabilities.NEW_POSITIONING);
        // cap.add(Capabilities.NEW_GRASPING);
        // cap.add(Capabilities.NEW_PLACING);
        // cap.add(Capabilities.NEW_BULK_FEEDING);
        // cap.add(Capabilities.NEW_TRAY_FEEDING);
        // cap.add(Capabilities.NEW_TUBE_FEEDING);
        // cap.add(Capabilities.NEW_TAPE_FEEDING);
		// 60

        return cap;
    }
}
