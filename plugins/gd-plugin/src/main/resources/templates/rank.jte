@param String user
@param String labelB20
@param String score
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
            <stop offset="0" stop-color="#4C8DFF" stop-opacity="0.20"/>
            <stop offset="1" stop-color="#4C8DFF" stop-opacity="0"/>
        </linearGradient>
    </defs>

    <rect width="960" height="${height}" fill="#0B0D12"/>
    <rect width="960" height="${height}" fill="url(#glow)"/>
    <rect x="0" y="0" width="6" height="${height}" fill="#4C8DFF"/>

    <text x="40" y="50" font-family="JetBrains Maple Mono" font-weight="bold" font-size="13"
          letter-spacing="2.4" fill="#8FA0B8"># GD / B20_</text>
    <text x="38" y="100" font-family="JetBrains Maple Mono" font-size="24"
          fill="#FFFFFF">${user}</text>
    <text x="40" y="142" font-family="JetBrains Maple Mono" font-size="13"
          fill="#8FA0B8">${labelB20}</text>
    <text x="38" y="198" font-family="JetBrains Maple Mono" font-weight="bold" font-size="54"
          fill="#4C8DFF">${score}</text>
    <text x="40" y="232" font-family="JetBrains Maple Mono" font-size="14"
          fill="#E7ECF5">${stats1}</text>
    <text x="40" y="258" font-family="JetBrains Maple Mono" font-size="14"
          fill="#C5CCDA">${stats2}</text>
    <rect x="40" y="282" width="880" height="1" fill="#232A36"/>

    <text x="40" y="310" font-family="JetBrains Maple Mono" font-weight="bold" font-size="12"
          fill="#8FA0B8">#</text>
    <text x="76" y="310" font-family="JetBrains Maple Mono" font-weight="bold" font-size="12"
          fill="#8FA0B8">${headLevel}</text>
    <text x="500" y="310" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="12" fill="#8FA0B8">ID</text>
    <text x="590" y="310" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="12" fill="#8FA0B8">${headTier}</text>
    <text x="700" y="310" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="12" fill="#8FA0B8">${headProgress}</text>
    <text x="800" y="310" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="12" fill="#8FA0B8">rt</text>

    @for(int i = 0; i < ranks.size(); i++)
        <text x="40" y="${346 + i * 32}" font-family="JetBrains Maple Mono" font-size="14"
              fill="#E7ECF5">${ranks.get(i)}</text>
        <text x="76" y="${346 + i * 32}" font-family="JetBrains Maple Mono" font-size="14"
              fill="#E7ECF5">${names.get(i)}</text>
        <text x="500" y="${346 + i * 32}" text-anchor="end" font-family="JetBrains Maple Mono"
              font-size="14" fill="#E7ECF5">${ids.get(i)}</text>
        <text x="590" y="${346 + i * 32}" text-anchor="end" font-family="JetBrains Maple Mono"
              font-size="14" fill="#E7ECF5">${tiers.get(i)}</text>
        <text x="700" y="${346 + i * 32}" text-anchor="end" font-family="JetBrains Maple Mono"
              font-size="14" fill="#E7ECF5">${progresses.get(i)}</text>
        <text x="800" y="${346 + i * 32}" text-anchor="end" font-family="JetBrains Maple Mono"
              font-size="14" fill="#E7ECF5">${rts.get(i)}</text>
    @endfor

    <text x="40" y="${height - 36}" font-family="JetBrains Maple Mono" font-size="12"
          fill="#8FA0B8">${note}</text>
    <text x="920" y="${height - 36}" text-anchor="end" font-family="JetBrains Maple Mono"
          font-weight="bold" font-size="11" letter-spacing="1.2" fill="#4C8DFF">GDDL</text>
</svg>
