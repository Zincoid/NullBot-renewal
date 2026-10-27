@param String title
@param String levelId
@param String meta
@param java.util.List<com.zincoid.gd.render.GdImagePresenter.StatCard> stats
@param com.zincoid.gd.render.GdImagePresenter.GddlCard gddl
@param String songLabel
@param String songValue
@param String descriptionLabel
@param java.util.List<com.zincoid.nullbot.module.render.svg.layout.SvgPositionedLine> descriptionLines
@param String thumbnail
@param String noThumbnail
@param int height

<svg width="1440" height="${height * 3 / 2}" viewBox="0 0 960 ${height}" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.035"/>
            <stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
        </linearGradient>
        <clipPath id="thumbnailClip">
            <rect x="40" y="170" width="400" height="225" rx="10"/>
        </clipPath>
    </defs>

    <rect width="960" height="${height}" fill="#0E0F11"/>
    <rect width="960" height="${height}" fill="url(#glow)"/>

    <text x="40" y="50" font-family="JetBrains Maple Mono" font-weight="bold" font-size="14"
          letter-spacing="2.4" fill="#C9CFD8"># GD / LEVEL_</text>
    <text x="38" y="100" font-family="JetBrains Maple Mono" font-weight="bold" font-size="32"
          fill="#F2F3F5">${title}</text>
    <rect x="820" y="76" width="100" height="28" rx="14" fill="#17181A" stroke="#2A2D33"/>
    <text x="870" y="94" text-anchor="middle" font-family="JetBrains Maple Mono"
          font-size="13" fill="#C9CFD8">#${levelId}</text>
    <text x="40" y="132" font-family="JetBrains Maple Mono" font-size="15"
          fill="#CDD3DC">${meta}</text>
    <rect x="40" y="148" width="880" height="1" fill="#2A2D33"/>

    <rect x="40" y="170" width="400" height="225" rx="10" fill="#17181A" stroke="#2A2D33"/>
    @if(!thumbnail.isBlank())
        <image x="40" y="170" width="400" height="225" preserveAspectRatio="xMidYMid slice"
               clip-path="url(#thumbnailClip)" href="${thumbnail}"/>
    @else
        <text x="240" y="290" text-anchor="middle" font-family="JetBrains Maple Mono"
              font-size="15" fill="#C9CFD8">${noThumbnail}</text>
    @endif

    @for(com.zincoid.gd.render.GdImagePresenter.StatCard stat : stats)
        <rect x="${stat.x()}" y="${stat.y()}" width="218" height="64" rx="8"
              fill="#17181A" stroke="#2A2D33"/>
        <text x="${stat.x() + 18}" y="${stat.y() + 22}" font-family="JetBrains Maple Mono"
              font-size="11" letter-spacing="0.8" fill="#8F98A3">${stat.label()}</text>
        <text x="${stat.x() + 18}" y="${stat.y() + 48}" font-family="JetBrains Maple Mono"
              font-size="20" font-weight="bold" fill="#F2F3F5">${stat.value()}</text>
    @endfor

    @if(!songValue.isBlank())
        <text x="464" y="344" font-family="JetBrains Maple Mono" font-size="11"
              letter-spacing="0.8" fill="#8F98A3">${songLabel}</text>
        <text x="464" y="368" font-family="JetBrains Maple Mono" font-size="15"
              fill="#C9CFD8">${songValue}</text>
    @endif

    <rect x="40" y="415" width="400" height="86" rx="10" fill="#141518" stroke="#2A2D33"/>
    <rect x="56" y="431" width="112" height="54" rx="8" fill="${gddl.accent()}"
          fill-opacity="0.14" stroke="${gddl.accent()}" stroke-opacity="0.45"/>
    <text x="112" y="447" text-anchor="middle" font-family="JetBrains Maple Mono"
          font-size="10" fill="${gddl.accent()}">${gddl.label()}</text>
    <text x="112" y="476" text-anchor="middle" font-family="JetBrains Maple Mono"
          font-size="26" font-weight="bold" fill="#F2F3F5">${gddl.tier()}</text>
    <text x="190" y="452" font-family="JetBrains Maple Mono" font-size="15"
          fill="#E6E9EE">${gddl.main()}</text>
    @if(!gddl.extra().isBlank())
        <text x="190" y="476" font-family="JetBrains Maple Mono" font-size="13"
              fill="#8F98A3">${gddl.extra()}</text>
    @endif

    @if(!descriptionLines.isEmpty())
        <text x="464" y="405" font-family="JetBrains Maple Mono" font-size="11"
              letter-spacing="0.8" fill="#8F98A3">${descriptionLabel}</text>
        @for(com.zincoid.nullbot.module.render.svg.layout.SvgPositionedLine line : descriptionLines)
            <text x="464" y="${line.y()}" font-family="JetBrains Maple Mono"
                  font-size="${line.size()}" fill="${line.color()}">${line.text()}</text>
        @endfor
    @endif

    <text x="40" y="${height - 40}" font-family="JetBrains Maple Mono" font-weight="bold"
          font-size="12" letter-spacing="1.2" fill="#C9CFD8">GDDL / NULLBOT</text>
    <text x="920" y="${height - 40}" text-anchor="end" font-family="JetBrains Maple Mono"
          font-size="13" fill="#C9CFD8">GD LEVEL</text>
</svg>
