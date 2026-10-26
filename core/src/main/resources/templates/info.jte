@param String app
@param String version
@param String build
@param String javaVersion
@param String spring
@param String uptime
@param String os
@param String time

<svg width="840" height="615" viewBox="0 0 560 410" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.035"/>
            <stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
        </linearGradient>
    </defs>

    <rect width="560" height="410" fill="#0E0F11"/>
    <rect width="560" height="410" fill="url(#glow)"/>

    <text x="34" y="52" font-family="JetBrains Maple Mono" font-weight="bold" font-size="13" letter-spacing="2.4"
          fill="#C9CFD8"># SYSTEM INFO_</text>
    <text x="32" y="105" font-family="JetBrains Maple Mono" font-weight="bold" font-size="32"
          fill="#F2F3F5">${app}</text>
    <text x="34" y="132" font-family="JetBrains Maple Mono" font-size="15"
          fill="#CDD3DC">${version}</text>
    <rect x="34" y="154" width="492" height="1" fill="#2A2D33"/>

    <text x="34" y="186" font-family="JetBrains Maple Mono" font-weight="bold"
          font-size="14" letter-spacing="1.2" fill="#C9CFD8">BUILD</text>
    <text x="180" y="186" font-family="JetBrains Maple Mono" font-size="14"
          fill="#E6E9EE">${build}</text>

    <text x="34" y="222" font-family="JetBrains Maple Mono" font-weight="bold"
          font-size="14" letter-spacing="1.2" fill="#C9CFD8">JAVA</text>
    <text x="180" y="222" font-family="JetBrains Maple Mono" font-size="14"
          fill="#E6E9EE">${javaVersion}</text>

    <text x="34" y="258" font-family="JetBrains Maple Mono" font-weight="bold"
          font-size="14" letter-spacing="1.2" fill="#C9CFD8">SPRING BOOT</text>
    <text x="180" y="258" font-family="JetBrains Maple Mono" font-size="14"
          fill="#E6E9EE">${spring}</text>

    <text x="34" y="294" font-family="JetBrains Maple Mono" font-weight="bold"
          font-size="14" letter-spacing="1.2" fill="#C9CFD8">UPTIME</text>
    <text x="180" y="294" font-family="JetBrains Maple Mono" font-size="14"
          fill="#E6E9EE">${uptime}</text>

    <text x="34" y="330" font-family="JetBrains Maple Mono" font-weight="bold"
          font-size="14" letter-spacing="1.2" fill="#C9CFD8">OS</text>
    <text x="180" y="330" font-family="JetBrains Maple Mono" font-size="14"
          fill="#E6E9EE">${os}</text>

    <text x="34" y="366" font-family="JetBrains Maple Mono" font-weight="bold"
          font-size="14" letter-spacing="1.2" fill="#C9CFD8">TIME</text>
    <text x="180" y="366" font-family="JetBrains Maple Mono" font-size="14"
          fill="#E6E9EE">${time}</text>
</svg>
