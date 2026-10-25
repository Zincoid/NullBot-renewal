@param String app
@param String version
@param String build
@param String javaVersion
@param String spring
@param String uptime
@param String os
@param String time

<svg width="560" height="400" viewBox="0 0 560 400" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#4C8DFF" stop-opacity="0.20"/>
            <stop offset="1" stop-color="#4C8DFF" stop-opacity="0"/>
        </linearGradient>
    </defs>

    <rect width="560" height="400" fill="#0B0D12"/>
    <rect width="560" height="400" fill="url(#glow)"/>
    <rect x="0" y="0" width="6" height="400" fill="#4C8DFF"/>

    <text x="34" y="52" font-family="JetBrains Mono Bold" font-size="13" letter-spacing="2.4"
          fill="#8FA0B8"># SYSTEM INFO_</text>
    <text x="32" y="105" font-family="JetBrains Mono Bold" font-size="32"
          fill="#FFFFFF">${app}</text>
    <text x="34" y="132" font-family="Noto Sans Mono CJK SC" font-size="15"
          fill="#C5CCDA">${version}</text>
    <rect x="34" y="154" width="492" height="1" fill="#232A36"/>

    <text x="34" y="186" font-family="JetBrains Mono Bold" font-size="11" letter-spacing="1.2"
          fill="#8FA0B8">BUILD</text>
    <text x="180" y="186" font-family="Noto Sans Mono CJK SC" font-size="14"
          fill="#E7ECF5">${build}</text>

    <text x="34" y="222" font-family="JetBrains Mono Bold" font-size="11" letter-spacing="1.2"
          fill="#8FA0B8">JAVA</text>
    <text x="180" y="222" font-family="Noto Sans Mono CJK SC" font-size="14"
          fill="#E7ECF5">${javaVersion}</text>

    <text x="34" y="258" font-family="JetBrains Mono Bold" font-size="11" letter-spacing="1.2"
          fill="#8FA0B8">SPRING BOOT</text>
    <text x="180" y="258" font-family="Noto Sans Mono CJK SC" font-size="14"
          fill="#E7ECF5">${spring}</text>

    <text x="34" y="294" font-family="JetBrains Mono Bold" font-size="11" letter-spacing="1.2"
          fill="#8FA0B8">UPTIME</text>
    <text x="180" y="294" font-family="Noto Sans Mono CJK SC" font-size="14"
          fill="#E7ECF5">${uptime}</text>

    <text x="34" y="330" font-family="JetBrains Mono Bold" font-size="11" letter-spacing="1.2"
          fill="#8FA0B8">OS</text>
    <text x="180" y="330" font-family="Noto Sans Mono CJK SC" font-size="13"
          fill="#E7ECF5">${os}</text>

    <text x="34" y="366" font-family="JetBrains Mono Bold" font-size="11" letter-spacing="1.2"
          fill="#8FA0B8">TIME</text>
    <text x="180" y="366" font-family="Noto Sans Mono CJK SC" font-size="14"
          fill="#E7ECF5">${time}</text>
</svg>
