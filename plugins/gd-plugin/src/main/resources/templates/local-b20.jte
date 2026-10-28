@param String user
@param String userId
@param String labelB20
@param String score
@param String avatar
@param String stats1
@param String stats2
@param java.util.List<com.zincoid.nullbot.module.render.svg.layout.SvgCell> cells
@param String note
@param int height

<svg width="1440" height="${height * 3 / 2}" viewBox="0 0 960 ${height}" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.035"/>
            <stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
        </linearGradient>
        <clipPath id="avatarClip">
            <rect x="840" y="36" width="80" height="80" rx="16"/>
        </clipPath>
    </defs>

    <rect width="960" height="${height}" fill="#0E0F11"/>
    <rect width="960" height="${height}" fill="url(#glow)"/>

    <text x="40" y="50" font-family="JetBrains Maple Mono" font-weight="bold" font-size="14"
          letter-spacing="2.4" fill="#C9CFD8"># GD / LOCAL B20_</text>
    @if(!avatar.isBlank())
        <image x="840" y="36" width="80" height="80" preserveAspectRatio="xMidYMid slice"
               clip-path="url(#avatarClip)" href="${avatar}"/>
        <rect x="840" y="36" width="80" height="80" rx="16" fill="none" stroke="#2A2D33"/>
    @else
        <rect x="840" y="36" width="80" height="80" rx="16" fill="#17181A" stroke="#2A2D33"/>
        <text x="880" y="91" text-anchor="middle" font-family="JetBrains Maple Mono"
              font-size="28" fill="#C9CFD8">&#xF007;</text>
    @endif
    <text x="38" y="100" font-family="JetBrains Maple Mono" font-weight="bold" font-size="32"
          fill="#F2F3F5">${user}<tspan font-size="20"> ${userId}</tspan></text>
    <text x="40" y="142" font-family="JetBrains Maple Mono" font-size="14"
          fill="#C9CFD8">${labelB20}</text>
    <text x="38" y="198" font-family="JetBrains Maple Mono" font-weight="bold" font-size="56"
          fill="#A7C6FF">${score}</text>
    <text x="40" y="232" font-family="JetBrains Maple Mono" font-size="16"
          fill="#E6E9EE">${stats1}</text>
    <text x="40" y="258" font-family="JetBrains Maple Mono" font-size="16"
          fill="#CDD3DC">${stats2}</text>
    <rect x="40" y="282" width="880" height="1" fill="#2A2D33"/>

    @for(com.zincoid.nullbot.module.render.svg.layout.SvgCell cell : cells)
        <text x="${cell.x()}" y="${cell.y()}" text-anchor="${cell.anchor()}"
              font-family="JetBrains Maple Mono" font-size="${cell.size()}"
              fill="${cell.color()}">${cell.text()}</text>
    @endfor

    <text x="40" y="${height - 36}" font-family="JetBrains Maple Mono" font-size="14"
          fill="#C9CFD8">${note}</text>
    <text x="903" y="${height - 36}" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="12" letter-spacing="1.2" fill="#C9CFD8">LOCAL / NULLBOT</text>
</svg>
