import { useState } from "react"

type IconName = "home" | "route" | "history" | "heart" | "user" | "bell" | "cloud" | "play" | "pause" | "skip" | "chevron" | "clock" | "pin" | "sparkle" | "close" | "car" | "music" | "share" | "check"

const paths: Record<IconName, React.ReactNode> = {
  home: (
    <>
      <path d="M3 10.8 12 3l9 7.8" />
      <path d="M5 9.5V21h14V9.5M9 21v-7h6v7" />
    </>
  ),
  route: (
    <>
      <circle cx="6" cy="19" r="2" />
      <circle cx="18" cy="5" r="2" />
      <path d="M7.8 18c5.5-1 2.2-9 7.8-11.8" />
    </>
  ),
  history: (
    <>
      <path d="M3 12a9 9 0 1 0 3-6.7L3 8" />
      <path d="M3 3v5h5M12 7v5l3 2" />
    </>
  ),
  heart: (
    <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.5 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z" />
  ),
  user: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </>
  ),
  bell: (
    <>
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
      <path d="M10 21h4" />
    </>
  ),
  cloud: (
    <>
      <path d="M7 18h10a4 4 0 0 0 .7-7.9A6 6 0 0 0 6.3 8.4 4.8 4.8 0 0 0 7 18Z" />
      <path d="m8 21-1 2m5-2-1 2m5-2-1 2" />
    </>
  ),
  play: <path d="m8 5 11 7-11 7Z" />,
  pause: (
    <>
      <path d="M9 5v14M15 5v14" />
    </>
  ),
  skip: (
    <>
      <path d="m6 5 9 7-9 7ZM18 5v14" />
    </>
  ),
  chevron: <path d="m9 18 6-6-6-6" />,
  clock: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  pin: (
    <>
      <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" />
      <circle cx="12" cy="10" r="2.5" />
    </>
  ),
  sparkle: (
    <>
      <path d="m12 3 1.2 4.1L17 9l-3.8 1.9L12 15l-1.2-4.1L7 9l3.8-1.9Z" />
      <path d="m19 15 .6 2.1L22 18l-2.4.9L19 21l-.6-2.1L16 18l2.4-.9Z" />
    </>
  ),
  close: (
    <>
      <path d="m6 6 12 12M18 6 6 18" />
    </>
  ),
  car: (
    <>
      <path d="m5 16-1 2v2h2v-2h12v2h2v-2l-1-2-2-7H7Z" />
      <path d="M6 14h12M8 18h.01M16 18h.01" />
    </>
  ),
  music: (
    <>
      <path d="M9 18V5l10-2v13" />
      <circle cx="6" cy="18" r="3" />
      <circle cx="16" cy="16" r="3" />
    </>
  ),
  share: (
    <>
      <circle cx="18" cy="5" r="2" />
      <circle cx="6" cy="12" r="2" />
      <circle cx="18" cy="19" r="2" />
      <path d="m8 11 8-5M8 13l8 5" />
    </>
  ),
  check: <path d="m5 12 4 4L19 6" />,
}

function Icon({
  name,
  size = 22,
  filled = false,
  className = "",
}: {
  name: IconName
  size?: number
  filled?: boolean
  className?: string
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill={filled ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      {paths[name]}
    </svg>
  )
}

const routes = [
  {
    id: 1,
    name: "북한강 물빛 드라이브",
    area: "양평 · 청평",
    time: "2시간 20분",
    distance: "86 km",
    tags: ["강변", "한적한"],
    image:
      "https://images.unsplash.com/photo-1761775607993-cda287a4e1e5?auto=format&fit=crop&w=900&q=85",
    color: "#a9c6d0",
  },
  {
    id: 2,
    name: "해안 절벽 파노라마",
    area: "강릉 · 정동진",
    time: "2시간 50분",
    distance: "112 km",
    tags: ["바다", "일몰"],
    image:
      "https://images.unsplash.com/photo-1778352490319-76e76a21f0f8?auto=format&fit=crop&w=900&q=85",
    color: "#95aeba",
  },
  {
    id: 3,
    name: "초록 능선 와인딩",
    area: "가평 · 유명산",
    time: "2시간 10분",
    distance: "74 km",
    tags: ["숲길", "커브"],
    image:
      "https://images.unsplash.com/photo-1758384265569-f03bed6d0757?auto=format&fit=crop&w=900&q=85",
    color: "#809783",
  },
]

const navItems: { id: string label: string icon: IconName }[] = [
  { id: "home", label: "홈", icon: "home" },
  { id: "route", label: "코스", icon: "route" },
  { id: "record", label: "기록", icon: "history" },
  { id: "saved", label: "저장", icon: "heart" },
  { id: "my", label: "마이", icon: "user" },
]

export default function App() {
  const [activeTab, setActiveTab] = useState("home")
  const [driveMode, setDriveMode] = useState(true)
  const [playing, setPlaying] = useState(false)
  const [favorites, setFavorites] = useState<number[]>([2])
  const [selectedRoute, setSelectedRoute] =
    useState<typeof routes[number] | null>(null)
  const [toast, setToast] = useState("")
  const [vehicleConnected, setVehicleConnected] = useState(false)
  const [vehicleModal, setVehicleModal] = useState(false)
  const [vehicleBrand, setVehicleBrand] = useState("현대")

  const notify = (message: string) => {
    setToast(message)
    window.setTimeout(() => setToast(""), 2200)
  }

  const toggleFavorite = (id: number) => {
    setFavorites((current) =>
      current.includes(id)
        ? current.filter((item) => item !== id)
        : [...current, id],
    )
  }

  return (
    <div className="app-shell">
      <main className="phone-frame">
        {activeTab === "home" && (
          <>
            <header className="topbar">
              <div>
                <p className="eyebrow">GOOD AFTERNOON</p>
                <h1>민준님, 오늘은 어디로 갈까요?</h1>
              </div>
              <button className="icon-button notification" aria-label="알림">
                <Icon name="bell" size={23} />
                <span />
              </button>
            </header>

            <section className="drive-status">
              <div className="drive-status__copy">
                <div className="status-label">
                  <span className={driveMode ? "pulse" : ""} />
                  {driveMode ? "드라이브 모드 ON" : "일반 주행 모드"}
                </div>
                <p>
                  {driveMode
                    ? "지금부터 풍경과 음악을 기록해요"
                    : "목적지까지 안전하게 안내할게요"}
                </p>
              </div>
              <button
                className={`switch ${driveMode ? "on" : ""}`}
                onClick={() => setDriveMode(!driveMode)}
                aria-label="드라이브 모드 전환"
              >
                <span />
              </button>
            </section>

            <button
              className={`vehicle-strip ${vehicleConnected ? "connected" : ""}`}
              onClick={() => setVehicleModal(true)}
            >
              <span className="vehicle-strip__icon">
                <Icon name={vehicleConnected ? "check" : "car"} size={18} />
              </span>
              <span>
                <strong>
                  {vehicleConnected
                    ? "IONIQ 5 · 실시간 연결 중"
                    : "내 차량을 연결해 주세요"}
                </strong>
                <small>
                  {vehicleConnected
                    ? "속도 · 주행 상태 · 배터리 데이터를 반영해요"
                    : "차량 데이터를 연결하면 추천이 더 정확해져요"}
                </small>
              </span>
              <Icon name="chevron" size={17} />
            </button>

            <section className="hero-card">
              <img
                src={routes[0].image}
                alt="산과 바다가 보이는 드라이브 코스"
              />
              <div className="hero-shade" />
              <div className="weather">
                <Icon name="cloud" size={17} />
                <span>18°</span>
                <small>가벼운 비</small>
              </div>
              <div className="hero-content">
                <div className="ai-label">
                  <Icon name="sparkle" size={15} /> 지금 당신에게 맞춘 코스
                </div>
                <h2>
                  비가 그친 뒤,
                  <br />
                  물빛 따라 달려보세요
                </h2>
                <p>
                  <Icon name="pin" size={15} /> 현 위치에서 18분 · 교통 원활
                </p>
                <button onClick={() => setSelectedRoute(routes[0])}>
                  코스 보기 <Icon name="chevron" size={16} />
                </button>
              </div>
            </section>

            <section className="section music-section">
              <div className="section-heading">
                <div>
                  <span className="overline">SPOTIFY MIX</span>
                  <h2>지금 이 순간의 플레이리스트</h2>
                </div>
                <button aria-label="플레이리스트 더보기">
                  <Icon name="chevron" size={20} />
                </button>
              </div>
              <div className="player">
                <div className="album-art">
                  <span className="disc" />
                  <div>
                    <Icon name="music" size={25} />
                  </div>
                </div>
                <div className="track">
                  <span>Rainy Road Mix</span>
                  <strong>Soft Drive</strong>
                  <small>비 오는 오후 · 여유로운 주행</small>
                </div>
                <button
                  className="play-button"
                  onClick={() => setPlaying(!playing)}
                  aria-label={playing ? "일시정지" : "재생"}
                >
                  <Icon
                    name={playing ? "pause" : "play"}
                    size={22}
                    filled={!playing}
                  />
                </button>
              </div>
              <div className="context-chips">
                <span># 빗길</span>
                <span># 42km/h</span>
                <span># 편안한 흐름</span>
              </div>
            </section>

            <section className="section route-section">
              <div className="section-heading">
                <div>
                  <span className="overline">FOR YOUR DRIVE</span>
                  <h2>2–3시간, 가볍게 다녀올 코스</h2>
                </div>
                <button onClick={() => setActiveTab("route")}>전체보기</button>
              </div>
              <div className="route-scroll">
                {routes.map((route) => (
                  <article
                    className="route-card"
                    key={route.id}
                    onClick={() => setSelectedRoute(route)}
                  >
                    <div className="route-image">
                      <img src={route.image} alt={route.name} />
                      <button
                        onClick={(event) => {
                          event.stopPropagation()
                          toggleFavorite(route.id)
                        }}
                        aria-label="즐겨찾기"
                      >
                        <Icon
                          name="heart"
                          size={20}
                          filled={favorites.includes(route.id)}
                        />
                      </button>
                      <div className="route-number">0{route.id}</div>
                    </div>
                    <div className="route-copy">
                      <div className="tags">
                        {route.tags.map((tag) => (
                          <span key={tag}>{tag}</span>
                        ))}
                      </div>
                      <h3>{route.name}</h3>
                      <p>{route.area}</p>
                      <div className="route-meta">
                        <span>
                          <Icon name="clock" size={14} />
                          {route.time}
                        </span>
                        <span>{route.distance}</span>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </section>

            <section
              className="memory-banner"
              onClick={() => setActiveTab("record")}
            >
              <div className="memory-art">
                <span className="road road-one" />
                <span className="road road-two" />
                <Icon name="car" size={28} />
              </div>
              <div>
                <span className="overline">2026 DRIVE STORY</span>
                <h3>
                  올해의 나는 지구를
                  <br />
                  <strong>0.3바퀴</strong> 달렸어요
                </h3>
                <p>12,480 km · 42번의 드라이브</p>
              </div>
              <Icon name="chevron" size={20} />
            </section>
          </>
        )}

        {activeTab === "route" && (
          <RoutePage
            favorites={favorites}
            toggleFavorite={toggleFavorite}
            select={setSelectedRoute}
          />
        )}
        {activeTab === "record" && <RecordPage />}
        {activeTab === "saved" && (
          <SavedPage
            favorites={favorites}
            toggleFavorite={toggleFavorite}
            select={setSelectedRoute}
          />
        )}
        {activeTab === "my" && (
          <MyPage
            vehicleConnected={vehicleConnected}
            openVehicle={() => setVehicleModal(true)}
          />
        )}

        <nav className="bottom-nav" aria-label="주요 메뉴">
          {navItems.map((item) => (
            <button
              key={item.id}
              className={activeTab === item.id ? "active" : ""}
              onClick={() => setActiveTab(item.id)}
            >
              <Icon
                name={item.icon}
                size={22}
                filled={activeTab === item.id && item.icon === "heart"}
              />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        {toast && (
          <div className="toast">
            <Icon name="check" size={17} />
            {toast}
          </div>
        )}
      </main>

      {selectedRoute && (
        <div className="modal-backdrop" onClick={() => setSelectedRoute(null)}>
          <section
            className="route-detail"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="detail-image">
              <img src={selectedRoute.image} alt={selectedRoute.name} />
              <button
                className="detail-close"
                onClick={() => setSelectedRoute(null)}
                aria-label="닫기"
              >
                <Icon name="close" />
              </button>
              <button
                className="detail-heart"
                onClick={() => toggleFavorite(selectedRoute.id)}
                aria-label="즐겨찾기"
              >
                <Icon
                  name="heart"
                  filled={favorites.includes(selectedRoute.id)}
                />
              </button>
            </div>
            <div className="detail-copy">
              <div className="tags">
                {selectedRoute.tags.map((tag) => (
                  <span key={tag}>{tag}</span>
                ))}
              </div>
              <h2>{selectedRoute.name}</h2>
              <p className="detail-area">
                <Icon name="pin" size={16} />
                {selectedRoute.area}
              </p>
              <div className="detail-stats">
                <div>
                  <small>예상 시간</small>
                  <strong>{selectedRoute.time}</strong>
                </div>
                <div>
                  <small>주행 거리</small>
                  <strong>{selectedRoute.distance}</strong>
                </div>
                <div>
                  <small>도로 혼잡</small>
                  <strong>여유</strong>
                </div>
              </div>
              <p className="detail-description">
                물과 숲이 번갈아 펼쳐지는 길을 따라 달리는 코스예요. 전망이 좋은
                세 곳을 경유하며, 잠시 쉬어가기 좋은 카페도 포함했어요.
              </p>
              <button
                className="primary-button"
                onClick={() => {
                  setSelectedRoute(null)
                  notify("드라이브 기록을 시작했어요")
                }}
              >
                <Icon name="car" size={20} /> 이 코스로 출발하기
              </button>
            </div>
          </section>
        </div>
      )}

      {vehicleModal && (
        <div className="modal-backdrop" onClick={() => setVehicleModal(false)}>
          <section
            className="vehicle-connect"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="vehicle-connect__header">
              <div>
                <span className="overline">CONNECTED CAR</span>
                <h2>{vehicleConnected ? "연결된 차량" : "내 차량 연결하기"}</h2>
              </div>
              <button onClick={() => setVehicleModal(false)} aria-label="닫기">
                <Icon name="close" size={21} />
              </button>
            </header>

            {vehicleConnected ? (
              <div className="connected-vehicle">
                <div className="vehicle-visual">
                  <Icon name="car" size={54} />
                  <span>CONNECTED</span>
                </div>
                <h3>IONIQ 5</h3>
                <p>12가 3456 · 마지막 동기화 방금 전</p>
                <div className="vehicle-data-grid">
                  <div>
                    <small>주행 속도</small>
                    <strong>42 km/h</strong>
                  </div>
                  <div>
                    <small>배터리</small>
                    <strong>74%</strong>
                  </div>
                  <div>
                    <small>주행 가능</small>
                    <strong>338 km</strong>
                  </div>
                </div>
                <div className="permission-note">
                  <Icon name="check" size={18} />
                  주행 중 필요한 데이터만 안전하게 수신하고 있어요.
                </div>
                <button
                  className="disconnect-button"
                  onClick={() => {
                    setVehicleConnected(false)
                    setVehicleModal(false)
                    notify("차량 연결을 해제했어요")
                  }}
                >
                  차량 연결 해제
                </button>
              </div>
            ) : (
              <>
                <p className="connect-description">
                  차량 계정 인증을 통해 속도와 주행 상태를 받아 상황에 맞는
                  음악과 코스를 추천해요.
                </p>
                <div className="connect-step">
                  <span>01</span>
                  <div>
                    <strong>차량 브랜드 선택</strong>
                    <p>이용 중인 커넥티드카 서비스를 선택해 주세요.</p>
                  </div>
                </div>
                <div className="brand-options">
                  {["현대", "제네시스", "기아"].map((brand) => (
                    <button
                      key={brand}
                      className={vehicleBrand === brand ? "active" : ""}
                      onClick={() => setVehicleBrand(brand)}
                    >
                      {brand}
                      {vehicleBrand === brand && (
                        <Icon name="check" size={15} />
                      )}
                    </button>
                  ))}
                </div>
                <div className="connect-step second">
                  <span>02</span>
                  <div>
                    <strong>데이터 이용 동의</strong>
                    <p>추천에 필요한 항목과 사용 목적을 확인해 주세요.</p>
                  </div>
                </div>
                <div className="data-permissions">
                  <div>
                    <Icon name="check" size={14} />
                    <span>
                      <strong>주행 상태</strong>
                      <small>속도, 시동, 급가감속</small>
                    </span>
                  </div>
                  <div>
                    <Icon name="check" size={14} />
                    <span>
                      <strong>차량 위치</strong>
                      <small>현재 위치, 이동 경로</small>
                    </span>
                  </div>
                  <div>
                    <Icon name="check" size={14} />
                    <span>
                      <strong>차량 상태</strong>
                      <small>연료·배터리, 주행 가능 거리</small>
                    </span>
                  </div>
                </div>
                <p className="privacy-copy">
                  데이터는 추천 및 주행 기록 생성에만 사용되며 언제든 연결을
                  해제할 수 있어요.
                </p>
                <button
                  className="primary-button"
                  onClick={() => {
                    setVehicleConnected(true)
                    setVehicleModal(false)
                    notify(`${vehicleBrand} 차량이 연결됐어요`)
                  }}
                >
                  <Icon name="car" size={20} />
                  {vehicleBrand} 계정으로 연결하기
                </button>
              </>
            )}
          </section>
        </div>
      )}
    </div>
  )
}

function PageHeader({ eyebrow, title }: { eyebrow: string title: string }) {
  return (
    <header className="page-header">
      <span className="overline">{eyebrow}</span>
      <h1>{title}</h1>
    </header>
  )
}

function RoutePage({
  favorites,
  toggleFavorite,
  select,
}: {
  favorites: number[]
  toggleFavorite: (id: number) => void
  select: (route: typeof routes[number]) => void
}) {
  const [filter, setFilter] = useState("전체")
  return (
    <div className="subpage">
      <PageHeader eyebrow="DISCOVER" title="오늘의 드라이브 코스" />
      <div className="filter-row">
        {["전체", "바다", "숲길", "한적한"].map((item) => (
          <button
            key={item}
            className={filter === item ? "active" : ""}
            onClick={() => setFilter(item)}
          >
            {item}
          </button>
        ))}
      </div>
      <div className="route-list">
        {routes
          .filter((route) => filter === "전체" || route.tags.includes(filter))
          .map((route) => (
            <article key={route.id} onClick={() => select(route)}>
              <img src={route.image} alt={route.name} />
              <div className="list-copy">
                <div className="tags">
                  {route.tags.map((tag) => (
                    <span key={tag}>{tag}</span>
                  ))}
                </div>
                <h3>{route.name}</h3>
                <p>
                  {route.area} · {route.time}
                </p>
              </div>
              <button
                onClick={(event) => {
                  event.stopPropagation()
                  toggleFavorite(route.id)
                }}
              >
                <Icon name="heart" filled={favorites.includes(route.id)} />
              </button>
            </article>
          ))}
      </div>
    </div>
  )
}

function RecordPage() {
  return (
    <div className="subpage record-page">
      <PageHeader eyebrow="MY DRIVE STORY" title="나의 주행 기록" />
      <div className="record-hero">
        <span>2026</span>
        <h2>
          12,480 <small>km</small>
        </h2>
        <p>
          당신이 만난 풍경은
          <br />
          하나의 길이 되었어요.
        </p>
        <div className="route-line">
          <i />
          <i />
          <i />
        </div>
      </div>
      <div className="stats-grid">
        <div>
          <small>드라이브</small>
          <strong>42회</strong>
        </div>
        <div>
          <small>함께한 시간</small>
          <strong>186시간</strong>
        </div>
        <div>
          <small>저장한 코스</small>
          <strong>18개</strong>
        </div>
      </div>
      <h3 className="list-title">최근 드라이브</h3>
      <div className="history-list">
        <div>
          <span>05. 18</span>
          <p>
            <strong>양평 물빛 드라이브</strong>맑음 · 86 km
          </p>
          <Icon name="chevron" size={18} />
        </div>
        <div>
          <span>05. 04</span>
          <p>
            <strong>서해 노을 해안길</strong>구름 · 124 km
          </p>
          <Icon name="chevron" size={18} />
        </div>
      </div>
    </div>
  )
}

function SavedPage({
  favorites,
  toggleFavorite,
  select,
}: {
  favorites: number[]
  toggleFavorite: (id: number) => void
  select: (route: typeof routes[number]) => void
}) {
  const saved = routes.filter((route) => favorites.includes(route.id))
  return (
    <div className="subpage">
      <PageHeader eyebrow="SAVED ROUTES" title="다시 가고 싶은 길" />
      <p className="page-description">
        가보고 싶은 길과 기억하고 싶은 코스를 모았어요.
      </p>
      <div className="route-list">
        {saved.length ? (
          saved.map((route) => (
            <article key={route.id} onClick={() => select(route)}>
              <img src={route.image} alt={route.name} />
              <div className="list-copy">
                <h3>{route.name}</h3>
                <p>
                  {route.area} · {route.time}
                </p>
              </div>
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  toggleFavorite(route.id)
                }}
              >
                <Icon name="heart" filled />
              </button>
            </article>
          ))
        ) : (
          <div className="empty-state">
            <Icon name="heart" size={34} />
            <h3>저장한 코스가 없어요</h3>
            <p>마음에 드는 코스에 하트를 눌러보세요.</p>
          </div>
        )}
      </div>
    </div>
  )
}

function MyPage({
  vehicleConnected,
  openVehicle,
}: {
  vehicleConnected: boolean
  openVehicle: () => void
}) {
  return (
    <div className="subpage">
      <PageHeader eyebrow="MY PROFILE" title="민준님의 드라이브" />
      <div className="profile-card">
        <div className="avatar">MJ</div>
        <div>
          <h3>김민준</h3>
          <p>blueway 드라이버 · Lv. 8</p>
        </div>
        <Icon name="chevron" size={18} />
      </div>
      <button
        className={`my-vehicle-card ${vehicleConnected ? "connected" : ""}`}
        onClick={openVehicle}
      >
        <span>
          <Icon name="car" size={25} />
        </span>
        <div>
          <small>MY VEHICLE</small>
          <strong>
            {vehicleConnected
              ? "IONIQ 5가 연결되어 있어요"
              : "내 차량 연결하기"}
          </strong>
          <p>
            {vehicleConnected
              ? "실시간 차량 데이터 수신 중"
              : "더 정확한 드라이브 추천을 받아보세요"}
          </p>
        </div>
        <Icon name="chevron" size={18} />
      </button>
      <h3 className="list-title">선호하는 드라이브</h3>
      <div className="preference-card">
        <span>바다</span>
        <span>한적한 길</span>
        <span>노을</span>
        <button>취향 다시 설정하기</button>
      </div>
      <div className="settings-list">
        <button>
          <span>
            <Icon name="music" />
            Spotify 연결
          </span>
          <em>연결됨</em>
        </button>
        <button>
          <span>
            <Icon name="bell" />
            주행 중 코스 추천
          </span>
          <em>켜짐</em>
        </button>
        <button>
          <span>
            <Icon name="share" />
            기록 공유 설정
          </span>
          <Icon name="chevron" size={17} />
        </button>
      </div>
    </div>
  )
}
