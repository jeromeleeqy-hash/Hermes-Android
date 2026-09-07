# 3.1.5 图标素材说明

- 最终应用资源：`app/src/main/res/drawable-nodpi/hermes_launcher_assistant.png`
- 素材来源：用户提供的 `523f85d1-4376-4c3c-a82f-4e14bc49422f.png`。
- 编辑方式：内置图像编辑工具，参考图编辑模式。保留棕色头发、蓝色耳机、蓝眼睛及托腮姿势，去掉棋盘格、青色圆形背景和装饰笔画，生成白底正方形资源。
- 白底为 `ic_launcher_background.xml` 中的 #FFFFFF；前景四边 24% inset，由 Android 原生自适应图标适配各启动器。
- 主页面原参考图及裁切坐标保持不变。导航图标使用此前的女孩 VectorDrawable，蓝紫渐变通过 Compose 绘制，不使用生成图片模拟交互。

## 编辑提示词

Use case: identity-preserve / precise-object-edit. Edit the supplied image into a production Android app launcher asset, square 1024x1024. Preserve this exact anime assistant girl: brown flowing hair with curled ends, blue eyes, cyan silver headphones, gentle smile, black outfit and hand resting on cheek. Preserve her face, expression, drawing style and pose faithfully. Replace ALL checkerboard background and the cyan circle behind her with completely plain pure white #FFFFFF. Remove the three detached cyan attention strokes at right. No gold, no helmet, no staff, no new character, no text, no frame, no watermark, no checkerboard. Recompose only by proportional scaling and centering the existing girl, do not redraw her pose. Center the whole portrait in the square and keep all hair tips, headphones and hand within a central circle of diameter 82% of canvas (leave white safe margins); keep her as large as fits this circle, shoulders may end in a softly rounded lower edge instead of a straight cutoff. This is the final flat artwork asset itself, not a phone, not an icon mockup, no rounded-square border or shadow.
