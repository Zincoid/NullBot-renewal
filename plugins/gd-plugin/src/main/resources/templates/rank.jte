@param String user
@param String labelB20
@param String score
@param String avatar
@param String stats1
@param String stats2
@param String headLevel
@param String headTier
@param String headProgress
@param java.util.List<String> ranks
@param java.util.List<String> names
@param java.util.List<String> ids
@param java.util.List<String> tiers
@param java.util.List<String> progresses
@param java.util.List<String> rts
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

    <text x="40" y="50" font-family="JetBrains Maple Mono" font-weight="bold" font-size="13"
          letter-spacing="2.4" fill="#C9CFD8"># GD / B20_</text>
    @if(!avatar.isBlank())
        <image x="840" y="36" width="80" height="80" preserveAspectRatio="xMidYMid slice"
               clip-path="url(#avatarClip)" href="${avatar}"/>
        <rect x="840" y="36" width="80" height="80" rx="16" fill="none" stroke="#2A2D33"/>
    @else
        <rect x="840" y="36" width="80" height="80" rx="16" fill="#17181A" stroke="#2A2D33"/>
        <text x="880" y="91" text-anchor="middle" font-family="JetBrains Maple Mono"
              font-size="28" fill="#C9CFD8">&#xF007;</text>
    @endif
    <text x="38" y="100" font-family="JetBrains Maple Mono" font-size="24"
          fill="#F2F3F5">${user}</text>
    <text x="40" y="142" font-family="JetBrains Maple Mono" font-size="13"
          fill="#C9CFD8">${labelB20}</text>
    <text x="38" y="198" font-family="JetBrains Maple Mono" font-weight="bold" font-size="54"
          fill="#A7C6FF">${score}</text>
    <text x="40" y="232" font-family="JetBrains Maple Mono" font-size="14"
          fill="#E6E9EE">${stats1}</text>
    <text x="40" y="258" font-family="JetBrains Maple Mono" font-size="14"
          fill="#CDD3DC">${stats2}</text>
    <rect x="40" y="282" width="880" height="1" fill="#2A2D33"/>

    <text x="40" y="310" font-family="JetBrains Maple Mono" font-weight="bold" font-size="12"
          fill="#C9CFD8">#</text>
    <text x="76" y="310" font-family="JetBrains Maple Mono" font-weight="bold" font-size="12"
          fill="#C9CFD8">${headLevel}</text>
    <text x="560" y="310" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="12" fill="#C9CFD8">ID</text>
    <text x="680" y="310" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="12" fill="#C9CFD8">${headTier}</text>
    <text x="800" y="310" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="12" fill="#C9CFD8">${headProgress}</text>
    <text x="920" y="310" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="12" fill="#C9CFD8">rt</text>

    @for(int i = 0; i < ranks.size(); i++)
        <text x="40" y="${346 + i * 32}" font-family="JetBrains Maple Mono" font-size="14"
              fill="#E6E9EE">${ranks.get(i)}</text>
        <text x="76" y="${346 + i * 32}" font-family="JetBrains Maple Mono" font-size="14"
              fill="#E6E9EE">${names.get(i)}</text>
        <text x="560" y="${346 + i * 32}" text-anchor="end" font-family="JetBrains Maple Mono"
              font-size="14" fill="#E6E9EE">${ids.get(i)}</text>
        <text x="680" y="${346 + i * 32}" text-anchor="end" font-family="JetBrains Maple Mono"
              font-size="14" fill="#E6E9EE">${tiers.get(i)}</text>
        <text x="800" y="${346 + i * 32}" text-anchor="end" font-family="JetBrains Maple Mono"
              font-size="14" fill="#E6E9EE">${progresses.get(i)}</text>
        <text x="920" y="${346 + i * 32}" text-anchor="end" font-family="JetBrains Maple Mono"
              font-size="14" fill="#E6E9EE">${rts.get(i)}</text>
    @endfor

    <text x="40" y="${height - 36}" font-family="JetBrains Maple Mono" font-size="12"
          fill="#C9CFD8">${note}</text>
    <text x="920" y="${height - 36}" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="11" letter-spacing="1.2" fill="#C9CFD8">GDDL</text>
</svg>
