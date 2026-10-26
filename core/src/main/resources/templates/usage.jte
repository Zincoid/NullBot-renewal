@param String avatar
@param String times

<svg width="840" height="480" viewBox="0 0 560 320" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#4C8DFF" stop-opacity="0.20"/>
            <stop offset="1" stop-color="#4C8DFF" stop-opacity="0"/>
        </linearGradient>
        <clipPath id="avatarClip">
            <rect x="50" y="120" width="160" height="160" rx="20"/>
        </clipPath>
    </defs>

    <rect width="560" height="320" fill="#0B0D12"/>
    <rect width="560" height="320" fill="url(#glow)"/>
    <rect x="0" y="0" width="6" height="320" fill="#4C8DFF"/>

    <text x="34" y="52" font-family="JetBrains Maple Mono" font-weight="bold" font-size="13" letter-spacing="2.4"
          fill="#8FA0B8"># USAGE_</text>
    <rect x="34" y="80" width="492" height="1" fill="#232A36"/>

    <image x="50" y="120" width="160" height="160" preserveAspectRatio="xMidYMid slice"
           clip-path="url(#avatarClip)" href="${avatar}"/>
    <rect x="50" y="120" width="160" height="160" rx="20" fill="none" stroke="#232A36"/>

    <text x="242" y="230" font-family="JetBrains Maple Mono" font-weight="bold" font-size="56"
          letter-spacing="0" fill="#FFFFFF">${times}</text>
    <text x="246" y="262" font-family="JetBrains Maple Mono" font-size="13" letter-spacing="1.6"
          fill="#C5CCDA">TOTAL COMMANDS</text>
</svg>
