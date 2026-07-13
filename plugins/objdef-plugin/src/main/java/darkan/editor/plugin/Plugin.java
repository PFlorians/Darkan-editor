package darkan.editor.plugin;

import darkan.editor.io.RSBuffer;
import darkan.editor.plugin.extension.ConfigExtension;

@PluginDescriptor(name = "Object Definition Plugin", authors = "Nshusa", version = "2.0.0")
public class Plugin extends ConfigExtension implements IPlugin {

  @Override
  public String applicationIcon() {
    return "icons/icon.png";
  }

  @Override
  public String fxml() {
    return "scene.fxml";
  }

  @Override
  public String[] stylesheets() {
    return new String[]{
        "css/style.css"
    };
  }

  @Override
  public String getFileName() {
    return "loc";
  }

  @Override
  public int getModernIndexId() {
    return 16;
  }

  @Override
  public int getModernBitShift() {
    return 8;
  }

  @Override
  protected void decode(int currentIndex, RSBuffer buffer) {
    int interactiveFlag = -1;
    id = currentIndex;

    while (true) {
      int opcode = buffer.readUByte();

      if (opcode == 0) {
        break;
      }

      if (opcode == 1 || opcode == 5) {
        int count = buffer.readUByte();
        modelTypes = new int[count];
        modelIds = new int[count][];
        for (int i = 0; i < count; i++) {
          modelTypes[i] = buffer.readByte();
          int modelCount = buffer.readUByte();
          modelIds[i] = new int[modelCount];
          for (int j = 0; j < modelCount; j++) {
            modelIds[i][j] = buffer.readBigSmart();
          }
        }
        if (opcode == 5) {
          // skip secondary model ids
          int skipCount = buffer.readUByte();
          for (int i = 0; i < skipCount; i++) {
            buffer.readByte(); // type
            int modelCount = buffer.readUByte();
            for (int j = 0; j < modelCount; j++) {
              buffer.readBigSmart();
            }
          }
        }
      } else if (opcode == 2) {
        name = buffer.readString();
      } else if (opcode == 14) {
        width = buffer.readUByte();
      } else if (opcode == 15) {
        length = buffer.readUByte();
      } else if (opcode == 17) {
        solid = false;
        blocks = false;
      } else if (opcode == 18) {
        blocks = false;
      } else if (opcode == 19) {
        interactiveFlag = buffer.readUByte();
        if (interactiveFlag == 1) {
          interactive = true;
        }
      } else if (opcode == 21) {
        contouredGround = true;
      } else if (opcode == 22) {
        delayShading = true;
      } else if (opcode == 23) {
        occludes = true;
      } else if (opcode == 24) {
        animation = buffer.readBigSmart();
      } else if (opcode == 27) {
        // clipType = 1
      } else if (opcode == 28) {
        decorDisplacement = buffer.readUByte() << 2;
      } else if (opcode == 29) {
        ambientLighting = buffer.readByte();
      } else if (opcode == 39) {
        lightDiffusion = buffer.readByte();
      } else if (opcode >= 30 && opcode < 35) {
        if (interactions == null) {
          interactions = new String[5];
        }
        interactions[opcode - 30] = buffer.readString();
        if (interactions[opcode - 30].equalsIgnoreCase("hidden")) {
          interactions[opcode - 30] = null;
        }
      } else if (opcode == 40) {
        int count = buffer.readUByte();
        originalColours = new int[count];
        replacementColours = new int[count];
        for (int i = 0; i < count; i++) {
          originalColours[i] = buffer.readUShort();
          replacementColours[i] = buffer.readUShort();
        }
      } else if (opcode == 41) {
        int count = buffer.readUByte();
        originalTextures = new int[count];
        replacementTextures = new int[count];
        for (int i = 0; i < count; i++) {
          originalTextures[i] = buffer.readUShort();
          replacementTextures[i] = buffer.readUShort();
        }
      } else if (opcode == 42) {
        int count = buffer.readUByte();
        for (int i = 0; i < count; i++) {
          buffer.readByte();
        }
      } else if (opcode == 44 || opcode == 45) {
        buffer.readUShort();
      } else if (opcode == 62) {
        inverted = true;
      } else if (opcode == 64) {
        castsShadow = false;
      } else if (opcode == 65) {
        scaleX = buffer.readUShort();
      } else if (opcode == 66) {
        scaleY = buffer.readUShort();
      } else if (opcode == 67) {
        scaleZ = buffer.readUShort();
      } else if (opcode == 69) {
        surroundings = buffer.readUByte();
      } else if (opcode == 70) {
        translateX = buffer.readShort();
      } else if (opcode == 71) {
        translateY = buffer.readShort();
      } else if (opcode == 72) {
        translateZ = buffer.readShort();
      } else if (opcode == 73) {
        obstructsGround = true;
      } else if (opcode == 74) {
        hollow = true;
      } else if (opcode == 75) {
        supportItems = buffer.readUByte();
      } else if (opcode == 77 || opcode == 92) {
        varbit = buffer.readUShort();
        if (varbit == 65535) {
          varbit = -1;
        }
        varp = buffer.readUShort();
        if (varp == 65535) {
          varp = -1;
        }
        int defaultId = -1;
        if (opcode == 92) {
          defaultId = buffer.readBigSmart();
        }
        int count = buffer.readUByte();
        morphisms = new int[count + 2];
        for (int i = 0; i <= count; i++) {
          morphisms[i] = buffer.readBigSmart();
        }
        morphisms[count + 1] = defaultId;
      } else if (opcode == 78) {
        buffer.readUShort(); // ambient sound id
        buffer.readUByte(); // hear distance
      } else if (opcode == 79) {
        buffer.readUShort();
        buffer.readUShort();
        buffer.readUByte();
        int count = buffer.readUByte();
        for (int i = 0; i < count; i++) {
          buffer.readUShort();
        }
      } else if (opcode == 81) {
        buffer.readUByte();
      } else if (opcode == 82) {
        // hidden = true
      } else if (opcode == 88) {
        // aBool5703 = false
      } else if (opcode == 89) {
        // randomizeAnimationStartFrame = false
      } else if (opcode == 91) {
        // members = true
      } else if (opcode == 93) {
        buffer.readUShort();
      } else if (opcode == 94) {
        // groundContoured = 4
      } else if (opcode == 95) {
        buffer.readShort();
      } else if (opcode == 97) {
        // adjustMapSceneRotation = true
      } else if (opcode == 98) {
        // hasAnimation = true
      } else if (opcode == 99) {
        buffer.readUByte();
        buffer.readUShort();
      } else if (opcode == 100) {
        buffer.readUByte();
        buffer.readUShort();
      } else if (opcode == 101) {
        buffer.readUByte(); // mapSpriteRotation
      } else if (opcode == 102) {
        mapscene = buffer.readUShort();
      } else if (opcode == 103) {
        // occludes = 0
      } else if (opcode == 104) {
        buffer.readUByte(); // ambientSoundVolume
      } else if (opcode == 105) {
        // flipMapSprite = true
      } else if (opcode == 106) {
        int count = buffer.readUByte();
        for (int i = 0; i < count; i++) {
          buffer.readBigSmart(); // animation id
          buffer.readUByte(); // probability
        }
      } else if (opcode == 107) {
        minimapFunction = buffer.readUShort();
      } else if (opcode >= 150 && opcode < 155) {
        if (interactions == null) {
          interactions = new String[5];
        }
        interactions[opcode - 150] = buffer.readString();
      } else if (opcode == 160) {
        int count = buffer.readUByte();
        for (int i = 0; i < count; i++) {
          buffer.readUShort();
        }
      } else if (opcode == 162) {
        buffer.readInt();
      } else if (opcode == 163) {
        buffer.readByte();
        buffer.readByte();
        buffer.readByte();
        buffer.readByte();
      } else if (opcode == 164) {
        buffer.readShort();
      } else if (opcode == 165) {
        buffer.readShort();
      } else if (opcode == 166) {
        buffer.readShort();
      } else if (opcode == 167) {
        buffer.readUShort();
      } else if (opcode == 168) {
        // bool
      } else if (opcode == 169) {
        // bool
      } else if (opcode == 170) {
        buffer.readUnsignedSmart();
      } else if (opcode == 171) {
        buffer.readUnsignedSmart();
      } else if (opcode == 173) {
        buffer.readUShort();
        buffer.readUShort();
      } else if (opcode == 177) {
        // bool
      } else if (opcode == 178) {
        buffer.readUByte();
      } else if (opcode == 186) {
        buffer.readUByte();
      } else if (opcode == 188) {
        // empty
      } else if (opcode == 189) {
        // bool
      } else if (opcode >= 190 && opcode < 196) {
        buffer.readUShort();
      } else if (opcode == 196 || opcode == 197) {
        buffer.readUByte();
      } else if (opcode == 198 || opcode == 199) {
        // empty
      } else if (opcode == 201) {
        buffer.readUnsignedSmart();
        buffer.readUnsignedSmart();
        buffer.readUnsignedSmart();
        buffer.readUnsignedSmart();
        buffer.readUnsignedSmart();
        buffer.readUnsignedSmart();
      } else if (opcode == 249) {
        int length = buffer.readUByte();
        for (int i = 0; i < length; i++) {
          boolean isString = buffer.readUByte() == 1;
          buffer.read24BitInt();
          if (isString) {
            buffer.readString();
          } else {
            buffer.readInt();
          }
        }
      }
    }

    if (interactiveFlag == -1) {
      interactive = interactions != null;
    }

    if (hollow) {
      solid = false;
      blocks = false;
    }

    if (supportItems == -1) {
      supportItems = solid ? 1 : 0;
    }
  }

  @Override
  protected void encode(RSBuffer buffer) {
    if (modelIds != null && modelTypes != null) {
      buffer.writeByte(1);
      buffer.writeByte(modelIds.length);
      for (int i = 0; i < modelIds.length; i++) {
        buffer.writeByte(modelTypes[i]);
        buffer.writeByte(modelIds[i].length);
        for (int j = 0; j < modelIds[i].length; j++) {
          buffer.writeShort(modelIds[i][j]);
        }
      }
    }

    if (name != null) {
      buffer.writeByte(2);
      buffer.writeString(name);
    }

    if (width != 1) {
      buffer.writeByte(14);
      buffer.writeByte(width);
    }

    if (length != 1) {
      buffer.writeByte(15);
      buffer.writeByte(length);
    }

    if (!solid) {
      buffer.writeByte(17);
    }

    if (!blocks) {
      buffer.writeByte(18);
    }

    if (interactive) {
      buffer.writeByte(19);
      buffer.writeByte(1);
    }

    if (contouredGround) {
      buffer.writeByte(21);
    }

    if (delayShading) {
      buffer.writeByte(22);
    }

    if (occludes) {
      buffer.writeByte(23);
    }

    if (animation != -1) {
      buffer.writeByte(24);
      buffer.writeShort(animation);
    }

    if (decorDisplacement != 0) {
      buffer.writeByte(28);
      buffer.writeByte(decorDisplacement >> 2);
    }

    if (ambientLighting != 0) {
      buffer.writeByte(29);
      buffer.writeByte(ambientLighting);
    }

    if (lightDiffusion != 0) {
      buffer.writeByte(39);
      buffer.writeByte(lightDiffusion);
    }

    if (interactions != null) {
      for (int i = 0; i < interactions.length; i++) {
        if (interactions[i] == null) {
          continue;
        }
        buffer.writeByte(30 + i);
        buffer.writeString(interactions[i]);
      }
    }

    if (originalColours != null && replacementColours != null) {
      buffer.writeByte(40);
      buffer.writeByte(originalColours.length);
      for (int i = 0; i < originalColours.length; i++) {
        buffer.writeShort(originalColours[i]);
        buffer.writeShort(replacementColours[i]);
      }
    }

    if (originalTextures != null && replacementTextures != null) {
      buffer.writeByte(41);
      buffer.writeByte(originalTextures.length);
      for (int i = 0; i < originalTextures.length; i++) {
        buffer.writeShort(originalTextures[i]);
        buffer.writeShort(replacementTextures[i]);
      }
    }

    if (inverted) {
      buffer.writeByte(62);
    }

    if (!castsShadow) {
      buffer.writeByte(64);
    }

    if (scaleX != 128) {
      buffer.writeByte(65);
      buffer.writeShort(scaleX);
    }

    if (scaleY != 128) {
      buffer.writeByte(66);
      buffer.writeShort(scaleY);
    }

    if (scaleZ != 128) {
      buffer.writeByte(67);
      buffer.writeShort(scaleZ);
    }

    if (surroundings != 0) {
      buffer.writeByte(69);
      buffer.writeByte(surroundings);
    }

    if (translateX != 0) {
      buffer.writeByte(70);
      buffer.writeShort(translateX);
    }

    if (translateY != 0) {
      buffer.writeByte(71);
      buffer.writeShort(translateY);
    }

    if (translateZ != 0) {
      buffer.writeByte(72);
      buffer.writeShort(translateZ);
    }

    if (obstructsGround) {
      buffer.writeByte(73);
    }

    if (hollow) {
      buffer.writeByte(74);
    }

    if (supportItems > 0) {
      buffer.writeByte(75);
      buffer.writeByte(supportItems);
    }

    if ((varbit != -1 || varp != -1) && morphisms != null) {
      buffer.writeByte(77);
      buffer.writeShort(varbit == -1 ? 65535 : varbit);
      buffer.writeShort(varp == -1 ? 65535 : varp);
      int count = morphisms.length - 2;
      buffer.writeByte(count);
      for (int i = 0; i <= count; i++) {
        buffer.writeShort(morphisms[i] == -1 ? 65535 : morphisms[i]);
      }
    }

    if (mapscene != -1) {
      buffer.writeByte(102);
      buffer.writeShort(mapscene);
    }

    if (minimapFunction != -1) {
      buffer.writeByte(107);
      buffer.writeShort(minimapFunction);
    }

    buffer.writeByte(0);
  }

  private byte ambientLighting;
  private int animation = -1;
  private boolean blocks = true;
  private boolean castsShadow = true;
  private boolean contouredGround;
  private int decorDisplacement;
  private boolean delayShading;
  private boolean hollow;
  private int id = -1;
  private boolean solid = true;
  private String[] interactions;
  private boolean interactive;
  private boolean inverted;
  private int length = 1;
  private byte lightDiffusion;
  private int mapscene = -1;
  private int minimapFunction = -1;
  private int[][] modelIds;
  private int[] modelTypes;
  private int[] morphisms;
  private int varbit = -1;
  private int varp = -1;
  private String name;
  private boolean obstructsGround;
  private boolean occludes;
  private int[] originalColours;
  private int[] replacementColours;
  private int[] originalTextures;
  private int[] replacementTextures;
  private int scaleX = 128;
  private int scaleY = 128;
  private int scaleZ = 128;
  private int supportItems = -1;
  private int surroundings;
  private int translateX;
  private int translateY;
  private int translateZ;
  private int width = 1;

}
