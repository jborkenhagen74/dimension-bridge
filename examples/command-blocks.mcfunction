# EINMALIG in Konsole/Chat ausführen:
scoreboard objectives add dimension trigger
scoreboard objectives add dimensionTarget dummy
scoreboard objectives add dimensionTimer dummy

# BEISPIEL: Ziel aus einem /trigger-Chatmenü übernehmen:
execute as @a[scores={dimension=1}] run scoreboard players set @s dimensionTarget 1
execute as @a[scores={dimension=2}] run scoreboard players set @s dimensionTarget 2
execute as @a[scores={dimension=3}] run scoreboard players set @s dimensionTarget 3
scoreboard players set @a[scores={dimension=1..3}] dimensionTimer 60
scoreboard players reset @a[scores={dimension=1..3}] dimension

# WIEDERHOLENDER BLOCK + KETTENBLÖCKE, alle "Immer aktiv":
execute as @a[scores={dimensionTimer=1..}] at @s run particle minecraft:portal ~ ~1 ~ 0.3 0.8 0.3 0.1 10 force
execute as @a[scores={dimensionTimer=1..}] at @s run particle minecraft:reverse_portal ~ ~1 ~ 0.45 0.9 0.45 0.08 5 force
execute as @a[scores={dimensionTimer=1..}] run title @s actionbar {"text":"Dimension wird geöffnet …","color":"aqua","italic":true}
execute as @a[scores={dimensionTimer=60}] at @s run playsound minecraft:block.beacon.activate master @s ~ ~ ~ 0.8 0.7
execute as @a[scores={dimensionTimer=20}] at @s run playsound minecraft:block.respawn_anchor.charge master @s ~ ~ ~ 0.8 1.3

# Transfers müssen VOR dem Herunterzählen von Timer 1 ausgeführt werden:
dimensionbridge transfer @a[scores={dimensionTimer=1,dimensionTarget=1}] conquest
dimensionbridge transfer @a[scores={dimensionTimer=1,dimensionTarget=2}] welt2
dimensionbridge transfer @a[scores={dimensionTimer=1,dimensionTarget=3}] welt262

scoreboard players remove @a[scores={dimensionTimer=2..}] dimensionTimer 1
scoreboard players reset @a[scores={dimensionTimer=1}] dimensionTarget
scoreboard players reset @a[scores={dimensionTimer=1}] dimensionTimer

# Einfachster Funktionstest in einem Impuls-Commandblock:
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] conquest

# Rückkehrzelle auf einem Spielserver:
dimensionbridge transfer @p[distance=..3,limit=1,sort=nearest] lobby
