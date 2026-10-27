@param String avatar
@param String user
@param String times

<svg width="840" height="480" viewBox="0 0 560 320" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.035"/>
            <stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
        </linearGradient>
        <clipPath id="avatarClip">
            <rect x="50" y="120" width="160" height="160" rx="20"/>
        </clipPath>
    </defs>

    <rect width="560" height="320" fill="#0E0F11"/>
    <rect width="560" height="320" fill="url(#glow)"/>

    <text x="34" y="52" font-family="JetBrains Maple Mono" font-weight="bold" font-size="13" letter-spacing="2.4"
          fill="#C9CFD8"># USAGE_</text>
    <rect x="34" y="80" width="492" height="1" fill="#2A2D33"/>

    @if(!avatar.isBlank())
        <image x="50" y="120" width="160" height="160" preserveAspectRatio="xMidYMid slice"
               clip-path="url(#avatarClip)" href="${avatar}"/>
        <rect x="50" y="120" width="160" height="160" rx="20" fill="none" stroke="#2A2D33"/>
    @else
        <rect x="50" y="120" width="160" height="160" rx="20" fill="#17181A" stroke="#2A2D33"/>
        <text x="130" y="228" text-anchor="middle" font-family="JetBrains Maple Mono"
              font-size="64" fill="#C9CFD8">&#xF007;</text>
    @endif

    <text x="242" y="170" font-family="JetBrains Maple Mono" font-size="16"
          fill="#E6E9EE">${user}</text>
    <text x="242" y="230" font-family="JetBrains Maple Mono" font-weight="bold" font-size="56"
          letter-spacing="0" fill="#A7C6FF">${times}</text>
    <text x="246" y="262" font-family="JetBrains Maple Mono" font-size="13" letter-spacing="1.6"
          fill="#CDD3DC">TOTAL COMMANDS</text>
</svg>
