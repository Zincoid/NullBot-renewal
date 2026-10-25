@param String avatar
@param String times

<svg width="560" height="320" viewBox="0 0 560 320" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#4C8DFF" stop-opacity="0.20"/>
            <stop offset="1" stop-color="#4C8DFF" stop-opacity="0"/>
        </linearGradient>
    </defs>

    <rect width="560" height="320" fill="#0B0D12"/>
    <rect width="560" height="320" fill="url(#glow)"/>
    <rect x="0" y="0" width="6" height="320" fill="#4C8DFF"/>

    <text x="34" y="52" font-family="JetBrains Mono Bold" font-size="13" letter-spacing="2.4"
          fill="#8FA0B8"># USAGE_</text>
    <rect x="34" y="80" width="492" height="1" fill="#232A36"/>

    <image x="34" y="110" width="160" height="160" preserveAspectRatio="xMidYMid slice"
           href="${avatar}"/>
    <rect x="34" y="110" width="160" height="160" fill="none" stroke="#232A36"/>

    <text x="226" y="220" font-family="JetBrains Mono Bold" font-size="56"
          letter-spacing="0" fill="#FFFFFF">${times}</text>
    <text x="230" y="252" font-family="JetBrains Mono Regular" font-size="13" letter-spacing="1.6"
          fill="#C5CCDA">TOTAL COMMANDS</text>
</svg>
