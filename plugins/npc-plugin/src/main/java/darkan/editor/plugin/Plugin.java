package darkan.editor.plugin;

import darkan.editor.io.RSBuffer;
import darkan.editor.plugin.extension.ConfigExtension;

import java.util.HashMap;
import java.util.Map;

@PluginDescriptor(name="NPC Definition Plugin", authors = "Nshusa", version = "2.0.0")
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
        return "npc";
    }

    @Override
    public int getModernIndexId() {
        return 18;
    }

    @Override
    public int getModernBitShift() {
        return 7;
    }

    @Override
    protected void decode(int currentIndex, RSBuffer buffer) {
        id = currentIndex;
        while(true) {
            int opcode = buffer.readUByte();

            if (opcode == 0) {
                break;
            }

            if (opcode == 1) {
                int count = buffer.readUByte();
                modelIds = new int[count];
                for (int i = 0; i < count; i++) {
                    modelIds[i] = buffer.readBigSmart();
                }
            } else if (opcode == 2) {
                name = buffer.readString();
            } else if (opcode == 12) {
                size = buffer.readUByte();
            } else if (opcode >= 30 && opcode < 35) {
                if (actions == null) {
                    actions = new String[5];
                }
                actions[opcode - 30] = buffer.readString();
                if (actions[opcode - 30].equalsIgnoreCase("hidden")) {
                    actions[opcode - 30] = null;
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
                recolourPalette = new byte[count];
                for (int i = 0; i < count; i++) {
                    recolourPalette[i] = buffer.readByte();
                }
            } else if (opcode == 60) {
                int count = buffer.readUByte();
                headModels = new int[count];
                for (int i = 0; i < count; i++) {
                    headModels[i] = buffer.readBigSmart();
                }
            } else if (opcode == 93) {
                drawMinimapDot = false;
            } else if (opcode == 95) {
                combat = buffer.readUShort();
            } else if (opcode == 97) {
                scaleXY = buffer.readUShort();
            } else if (opcode == 98) {
                scaleZ = buffer.readUShort();
            } else if (opcode == 99) {
                priorityRender = true;
            } else if (opcode == 100) {
                lightModifier = buffer.readByte();
            } else if (opcode == 101) {
                shadowModifier = buffer.readByte();
            } else if (opcode == 102) {
                headIcon = buffer.readUShort();
            } else if (opcode == 103) {
                rotation = buffer.readUShort();
            } else if (opcode == 106 || opcode == 118) {
                varbit = buffer.readUShort();
                if (varbit == 65535) {
                    varbit = -1;
                }
                varp = buffer.readUShort();
                if (varp == 65535) {
                    varp = -1;
                }
                int defaultId = -1;
                if (opcode == 118) {
                    defaultId = buffer.readUShort();
                    if (defaultId == 65535) {
                        defaultId = -1;
                    }
                }
                int count = buffer.readUByte();
                morphisms = new int[count + 2];
                for (int i = 0; i <= count; i++) {
                    morphisms[i] = buffer.readUShort();
                    if (morphisms[i] == 65535) {
                        morphisms[i] = -1;
                    }
                }
                morphisms[count + 1] = defaultId;
            } else if (opcode == 107) {
                clickable = false;
            } else if (opcode == 109) {
                // isClickable = false (rl variant)
                buffer.getClass(); // no-op, no data to read
            } else if (opcode == 111) {
                // animateIdle = false
            } else if (opcode == 113) {
                buffer.readUShort();
                buffer.readUShort();
            } else if (opcode == 114) {
                buffer.readByte();
                buffer.readByte();
            } else if (opcode == 119) {
                walkMask = buffer.readByte();
            } else if (opcode == 121) {
                modelTranslation = new int[modelIds != null ? modelIds.length : 0][];
                int count = buffer.readUByte();
                for (int i = 0; i < count; i++) {
                    int idx = buffer.readUByte();
                    if (modelTranslation.length > idx) {
                        int[] translations = new int[3];
                        translations[0] = buffer.readByte();
                        translations[1] = buffer.readByte();
                        translations[2] = buffer.readByte();
                        modelTranslation[idx] = translations;
                    } else {
                        buffer.readByte();
                        buffer.readByte();
                        buffer.readByte();
                    }
                }
            } else if (opcode == 123) {
                height = buffer.readUShort();
            } else if (opcode == 125) {
                respawnDirection = buffer.readByte();
            } else if (opcode == 127) {
                basId = buffer.readUShort();
            } else if (opcode == 128) {
                movementType = buffer.readUByte();
            } else if (opcode == 134) {
                walkingAnimation = buffer.readUShort();
                if (walkingAnimation == 65535) walkingAnimation = -1;
                halfTurnAnimation = buffer.readUShort();
                if (halfTurnAnimation == 65535) halfTurnAnimation = -1;
                rotateClockwiseAnimation = buffer.readUShort();
                if (rotateClockwiseAnimation == 65535) rotateClockwiseAnimation = -1;
                rotateAntiClockwiseAnimation = buffer.readUShort();
                if (rotateAntiClockwiseAnimation == 65535) rotateAntiClockwiseAnimation = -1;
                buffer.readUByte(); // specialByte
            } else if (opcode == 135) {
                buffer.readUByte();
                buffer.readUShort();
            } else if (opcode == 136) {
                buffer.readUByte();
                buffer.readUShort();
            } else if (opcode == 137) {
                attackOpCursor = buffer.readUShort();
            } else if (opcode == 138) {
                armyIcon = buffer.readBigSmart();
            } else if (opcode == 140) {
                buffer.readUByte();
            } else if (opcode == 141) {
                // bool flag
            } else if (opcode == 142) {
                mapIcon = buffer.readUShort();
            } else if (opcode == 143) {
                // bool flag
            } else if (opcode >= 150 && opcode < 155) {
                if (membersOptions == null) {
                    membersOptions = new String[5];
                }
                membersOptions[opcode - 150] = buffer.readString();
            } else if (opcode == 155) {
                buffer.readByte();
                buffer.readByte();
                buffer.readByte();
                buffer.readByte();
            } else if (opcode == 158) {
                // byte flag = 1
            } else if (opcode == 159) {
                // byte flag = 0
            } else if (opcode == 160) {
                int count = buffer.readUByte();
                quests = new int[count];
                for (int i = 0; i < count; i++) {
                    quests[i] = buffer.readUShort();
                }
            } else if (opcode == 162) {
                // bool flag
            } else if (opcode == 163) {
                buffer.readUByte();
            } else if (opcode == 164) {
                buffer.readUShort();
                buffer.readUShort();
            } else if (opcode == 165) {
                buffer.readUByte();
            } else if (opcode == 168) {
                buffer.readUByte();
            } else if (opcode == 169) {
                // bool flag
            } else if (opcode == 249) {
                int length = buffer.readUByte();
                for (int i = 0; i < length; i++) {
                    boolean isString = buffer.readUByte() == 1;
                    int key = buffer.read24BitInt();
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
        if (modelIds != null) {
            buffer.writeByte(1);
            buffer.writeByte(modelIds.length);
            for (int i = 0; i < modelIds.length; i++) {
                buffer.writeShort(modelIds[i]);
            }
        }

        if (name != null) {
            buffer.writeByte(2);
            buffer.writeString(name);
        }

        if (size != 1) {
            buffer.writeByte(12);
            buffer.writeByte(size);
        }

        if (actions != null) {
            for (int i = 0; i < actions.length; i++) {
                if (actions[i] == null) {
                    continue;
                }
                buffer.writeByte(30 + i);
                buffer.writeString(actions[i]);
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

        if (headModels != null) {
            buffer.writeByte(60);
            buffer.writeByte(headModels.length);
            for (int i = 0; i < headModels.length; i++) {
                buffer.writeShort(headModels[i]);
            }
        }

        if (!drawMinimapDot) {
            buffer.writeByte(93);
        }

        if (combat != -1) {
            buffer.writeByte(95);
            buffer.writeShort(combat);
        }

        if (scaleXY != 128) {
            buffer.writeByte(97);
            buffer.writeShort(scaleXY);
        }

        if (scaleZ != 128) {
            buffer.writeByte(98);
            buffer.writeShort(scaleZ);
        }

        if (priorityRender) {
            buffer.writeByte(99);
        }

        if (lightModifier != 0) {
            buffer.writeByte(100);
            buffer.writeByte(lightModifier);
        }

        if (shadowModifier != 0) {
            buffer.writeByte(101);
            buffer.writeByte(shadowModifier);
        }

        if (headIcon != -1) {
            buffer.writeByte(102);
            buffer.writeShort(headIcon);
        }

        if (rotation != 32) {
            buffer.writeByte(103);
            buffer.writeShort(rotation);
        }

        if ((varbit != -1 || varp != -1) && morphisms != null) {
            buffer.writeByte(106);
            buffer.writeShort(varbit == -1 ? 65535 : varbit);
            buffer.writeShort(varp == -1 ? 65535 : varp);
            int count = morphisms.length - 2;
            buffer.writeByte(count);
            for (int i = 0; i <= count; i++) {
                buffer.writeShort(morphisms[i] == -1 ? 65535 : morphisms[i]);
            }
        }

        if (!clickable) {
            buffer.writeByte(107);
        }

        buffer.writeByte(0);
    }

    private int[] headModels;
    private boolean clickable = true;
    private int combat = -1;
    private boolean drawMinimapDot = true;
    private int halfTurnAnimation = -1;
    private int headIcon = -1;
    private long id = -1;
    private int idleAnimation = -1;
    private String[] actions;
    private int lightModifier;
    private int[] modelIds;
    private int[] morphisms;
    private int varbit = -1;
    private int varp = -1;
    private String name;
    private int[] originalColours;
    private int[] replacementColours;
    private int[] originalTextures;
    private int[] replacementTextures;
    private byte[] recolourPalette;
    private boolean priorityRender = false;
    private int rotateAntiClockwiseAnimation = -1;
    private int rotateClockwiseAnimation = -1;
    private int rotation = 32;
    private int scaleXY = 128;
    private int scaleZ = 128;
    private int shadowModifier;
    private int size = 1;
    private int walkingAnimation = -1;
    private int[][] modelTranslation;
    private int height = -1;
    private byte respawnDirection = -1;
    private int basId = -1;
    private int movementType = -1;
    private int attackOpCursor = -1;
    private int armyIcon = -1;
    private int mapIcon = -1;
    private String[] membersOptions;
    private int[] quests;
    private byte walkMask = 0;
}
