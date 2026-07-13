package darkan.editor.plugin;

import darkan.editor.io.RSBuffer;
import darkan.editor.plugin.extension.ConfigExtension;

@PluginDescriptor(name = "Item Definition Plugin", authors = "Nshusa", version = "2.0.0")
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
    return "obj";
  }

  @Override
  public int getModernIndexId() {
    return 19;
  }

  @Override
  public int getModernBitShift() {
    return 8;
  }

  @Override
  protected void decode(int currentIndex, RSBuffer buffer) {
    this.id = currentIndex;
    while (true) {
      int opcode = buffer.readUByte();
      if (opcode == 0) {
        break;
      }

      if (opcode == 1) {
        modelId = buffer.readBigSmart();
      } else if (opcode == 2) {
        name = buffer.readString();
      } else if (opcode == 4) {
        spriteScale = buffer.readUShort();
      } else if (opcode == 5) {
        spritePitch = buffer.readUShort();
      } else if (opcode == 6) {
        spriteCameraRoll = buffer.readUShort();
      } else if (opcode == 7) {
        spriteTranslateX = buffer.readUShort();
        if (spriteTranslateX > 32767) {
          spriteTranslateX -= 0x10000;
        }
      } else if (opcode == 8) {
        spriteTranslateY = buffer.readUShort();
        if (spriteTranslateY > 32767) {
          spriteTranslateY -= 0x10000;
        }
      } else if (opcode == 11) {
        stackable = true;
      } else if (opcode == 12) {
        value = buffer.readInt();
      } else if (opcode == 13) {
        wearPos = buffer.readUByte();
      } else if (opcode == 14) {
        wearPos2 = buffer.readUByte();
      } else if (opcode == 16) {
        members = true;
      } else if (opcode == 18) {
        multiStackSize = buffer.readUShort();
      } else if (opcode == 23) {
        primaryMaleModel = buffer.readBigSmart();
      } else if (opcode == 24) {
        secondaryMaleModel = buffer.readBigSmart();
      } else if (opcode == 25) {
        primaryFemaleModel = buffer.readBigSmart();
      } else if (opcode == 26) {
        secondaryFemaleModel = buffer.readBigSmart();
      } else if (opcode == 27) {
        wearPos3 = buffer.readUByte();
      } else if (opcode >= 30 && opcode < 35) {
        if (groundActions == null) {
          groundActions = new String[5];
        }
        groundActions[opcode - 30] = buffer.readString();
      } else if (opcode >= 35 && opcode < 40) {
        if (widgetActions == null) {
          widgetActions = new String[5];
        }
        widgetActions[opcode - 35] = buffer.readString();
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
      } else if (opcode == 43) {
        buffer.readInt(); // tooltip color
      } else if (opcode == 44) {
        buffer.readUShort();
      } else if (opcode == 45) {
        buffer.readUShort();
      } else if (opcode == 65) {
        grandExchange = true;
      } else if (opcode == 78) {
        tertiaryMaleEquipmentModel = buffer.readBigSmart();
      } else if (opcode == 79) {
        tertiaryFemaleEquipmentModel = buffer.readBigSmart();
      } else if (opcode == 90) {
        primaryMaleHeadPiece = buffer.readBigSmart();
      } else if (opcode == 91) {
        primaryFemaleHeadPiece = buffer.readBigSmart();
      } else if (opcode == 92) {
        secondaryMaleHeadPiece = buffer.readBigSmart();
      } else if (opcode == 93) {
        secondaryFemaleHeadPiece = buffer.readBigSmart();
      } else if (opcode == 94) {
        buffer.readUShort();
      } else if (opcode == 95) {
        spriteCameraYaw = buffer.readUShort();
      } else if (opcode == 96) {
        buffer.readUByte();
      } else if (opcode == 97) {
        noteInfoId = buffer.readUShort();
      } else if (opcode == 98) {
        notedTemplateId = buffer.readUShort();
      } else if (opcode >= 100 && opcode < 110) {
        if (stackIds == null) {
          stackIds = new int[10];
          stackAmounts = new int[10];
        }
        stackIds[opcode - 100] = buffer.readUShort();
        stackAmounts[opcode - 100] = buffer.readUShort();
      } else if (opcode == 110) {
        groundScaleX = buffer.readUShort();
      } else if (opcode == 111) {
        groundScaleY = buffer.readUShort();
      } else if (opcode == 112) {
        groundScaleZ = buffer.readUShort();
      } else if (opcode == 113) {
        ambience = buffer.readByte();
      } else if (opcode == 114) {
        diffusion = buffer.readByte() * 5;
      } else if (opcode == 115) {
        team = buffer.readUByte();
      } else if (opcode == 121) {
        lendId = buffer.readUShort();
      } else if (opcode == 122) {
        lendTemplateId = buffer.readUShort();
      } else if (opcode == 125) {
        buffer.readByte();
        buffer.readByte();
        buffer.readByte();
      } else if (opcode == 126) {
        buffer.readByte();
        buffer.readByte();
        buffer.readByte();
      } else if (opcode == 127) {
        buffer.readUByte();
        buffer.readUShort();
      } else if (opcode == 128) {
        buffer.readUByte();
        buffer.readUShort();
      } else if (opcode == 129) {
        buffer.readUByte();
        buffer.readUShort();
      } else if (opcode == 130) {
        buffer.readUByte();
        buffer.readUShort();
      } else if (opcode == 132) {
        int count = buffer.readUByte();
        for (int i = 0; i < count; i++) {
          buffer.readUShort();
        }
      } else if (opcode == 134) {
        buffer.readUByte();
      } else if (opcode == 139) {
        bindId = buffer.readUShort();
      } else if (opcode == 140) {
        bindTemplateId = buffer.readUShort();
      } else if (opcode >= 142 && opcode < 147) {
        buffer.readUShort();
      } else if (opcode >= 150 && opcode < 155) {
        buffer.readUShort();
      } else if (opcode == 157) {
        // empty
      } else if (opcode == 161) {
        buffer.readUShort();
      } else if (opcode == 162) {
        buffer.readUShort();
      } else if (opcode == 163) {
        buffer.readUShort();
      } else if (opcode == 164) {
        buffer.readString();
      } else if (opcode == 165) {
        // stackable = 2
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
  }

  @Override
  protected void encode(RSBuffer buffer) {

    if (modelId != 0) {
      buffer.writeByte(1);
      buffer.writeShort(modelId);
    }

    if (name != null) {
      buffer.writeByte(2);
      buffer.writeString(name);
    }

    if (spriteScale != 2000) {
      buffer.writeByte(4);
      buffer.writeShort(spriteScale);
    }

    if (spritePitch != 0) {
      buffer.writeByte(5);
      buffer.writeShort(spritePitch);
    }

    if (spriteCameraRoll != 0) {
      buffer.writeByte(6);
      buffer.writeShort(spriteCameraRoll);
    }

    if (spriteTranslateX != 0) {
      buffer.writeByte(7);
      buffer.writeShort(spriteTranslateX);
    }

    if (spriteTranslateY != 0) {
      buffer.writeByte(8);
      buffer.writeShort(spriteTranslateY);
    }

    if (stackable) {
      buffer.writeByte(11);
    }

    if (value != 1) {
      buffer.writeByte(12);
      buffer.writeInt(value);
    }

    if (members) {
      buffer.writeByte(16);
    }

    if (primaryMaleModel != -1) {
      buffer.writeByte(23);
      buffer.writeShort(primaryMaleModel);
    }

    if (secondaryMaleModel != -1) {
      buffer.writeByte(24);
      buffer.writeShort(secondaryMaleModel);
    }

    if (primaryFemaleModel != -1) {
      buffer.writeByte(25);
      buffer.writeShort(primaryFemaleModel);
    }

    if (secondaryFemaleModel != -1) {
      buffer.writeByte(26);
      buffer.writeShort(secondaryFemaleModel);
    }

    if (groundActions != null) {
      for (int i = 0; i < groundActions.length; i++) {
        if (groundActions[i] == null) {
          continue;
        }
        buffer.writeByte(30 + i);
        buffer.writeString(groundActions[i]);
      }
    }

    if (widgetActions != null) {
      for (int i = 0; i < widgetActions.length; i++) {
        if (widgetActions[i] == null) {
          continue;
        }
        buffer.writeByte(35 + i);
        buffer.writeString(widgetActions[i]);
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

    if (grandExchange) {
      buffer.writeByte(65);
    }

    if (tertiaryMaleEquipmentModel != -1) {
      buffer.writeByte(78);
      buffer.writeShort(tertiaryMaleEquipmentModel);
    }

    if (tertiaryFemaleEquipmentModel != -1) {
      buffer.writeByte(79);
      buffer.writeShort(tertiaryFemaleEquipmentModel);
    }

    if (primaryMaleHeadPiece != -1) {
      buffer.writeByte(90);
      buffer.writeShort(primaryMaleHeadPiece);
    }

    if (primaryFemaleHeadPiece != -1) {
      buffer.writeByte(91);
      buffer.writeShort(primaryFemaleHeadPiece);
    }

    if (secondaryMaleHeadPiece != -1) {
      buffer.writeByte(92);
      buffer.writeShort(secondaryMaleHeadPiece);
    }

    if (secondaryFemaleHeadPiece != -1) {
      buffer.writeByte(93);
      buffer.writeShort(secondaryFemaleHeadPiece);
    }

    if (spriteCameraYaw != 0) {
      buffer.writeByte(95);
      buffer.writeShort(spriteCameraYaw);
    }

    if (noteInfoId != -1) {
      buffer.writeByte(97);
      buffer.writeShort(noteInfoId);
    }

    if (notedTemplateId != -1) {
      buffer.writeByte(98);
      buffer.writeShort(notedTemplateId);
    }

    if (stackIds != null && stackAmounts != null) {
      for (int i = 0; i < stackIds.length; i++) {
        buffer.writeByte(100 + i);
        buffer.writeShort(stackIds[i]);
        buffer.writeShort(stackAmounts[i]);
      }
    }

    if (groundScaleX != 128) {
      buffer.writeByte(110);
      buffer.writeShort(groundScaleX);
    }

    if (groundScaleY != 128) {
      buffer.writeByte(111);
      buffer.writeShort(groundScaleY);
    }

    if (groundScaleZ != 128) {
      buffer.writeByte(112);
      buffer.writeShort(groundScaleZ);
    }

    if (ambience != 0) {
      buffer.writeByte(113);
      buffer.writeByte(ambience);
    }

    if (diffusion != 0) {
      buffer.writeByte(114);
      buffer.writeByte(diffusion / 5);
    }

    if (team != 0) {
      buffer.writeByte(115);
      buffer.writeByte(team);
    }

    buffer.writeByte(0);
  }

  private int id = -1;
  private int modelId;
  private String name;
  private int spriteScale = 2000;
  private int spritePitch;
  private int spriteCameraRoll;
  private int spriteTranslateX;
  private int spriteTranslateY;
  private boolean stackable;
  private int value = 1;
  private int wearPos = -1;
  private int wearPos2 = -1;
  private int wearPos3 = -1;
  private boolean members;
  private int multiStackSize;
  private int primaryMaleModel = -1;
  private int secondaryMaleModel = -1;
  private int primaryFemaleModel = -1;
  private int secondaryFemaleModel = -1;
  private String[] groundActions;
  private String[] widgetActions;
  private int[] originalColours;
  private int[] replacementColours;
  private int[] originalTextures;
  private int[] replacementTextures;
  private boolean grandExchange;
  private int tertiaryMaleEquipmentModel = -1;
  private int tertiaryFemaleEquipmentModel = -1;
  private int primaryMaleHeadPiece = -1;
  private int primaryFemaleHeadPiece = -1;
  private int secondaryMaleHeadPiece = -1;
  private int secondaryFemaleHeadPiece = -1;
  private int spriteCameraYaw;
  private int noteInfoId = -1;
  private int notedTemplateId = -1;
  private int[] stackIds;
  private int[] stackAmounts;
  private int groundScaleX = 128;
  private int groundScaleY = 128;
  private int groundScaleZ = 128;
  private int ambience;
  private int diffusion;
  private int team;
  private int lendId = -1;
  private int lendTemplateId = -1;
  private int bindId = -1;
  private int bindTemplateId = -1;

}
