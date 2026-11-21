-- 예시 신랑·신부: 박서준 · 김다은 (가상 예시)
-- 예식장: 엘리에나호텔 / 서울특별시 강남구 논현로 645
-- 샘플 홀: 5층 그랜드볼룸
-- 건물 지도 좌표: 위도 37.5111132, 경도 127.0314126 (공개 주소 자료 기준)
-- 실제 지도 서비스의 장소 ID는 확인하지 않아 placeId=NULL, locationConfirmed=false
-- 확인하지 않은 무료 주차 시간 및 셔틀 운행 정보는 사용하지 않음
-- 템플릿 6종과 서버 청첩장 body 구조의 미리보기 데이터 등록
-- 신규 등록용: 중복 code가 있으면 실패하며 기존 행을 덮어쓰지 않음
-- preview_body 컬럼이 추가된 DB에서 실행
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO templates (code, name, subtitle, description, category, tags, cover_url, default_font_key, preview_body, is_selectable)
VALUES
-- 아워 모먼트 (our-moment)
(
    'our-moment',
    '아워 모먼트',
    '영화처럼 기억될 우리',
    '소중한 순간을 크게, 이야기는 담백하게. 사진과 선명한 타이포그래피로 두 사람만의 장면을 보여줘요.',
    'photo',
    JSON_ARRAY(
            '사진 중심',
            '모던'
    ),
    '/images/template-samples/color-transparent-v1/wedding-wave.png',
    'cafe24-oneprettynight',
    JSON_OBJECT(
            'font', 'cafe24-oneprettynight',
            'basic', JSON_OBJECT(
                    'groomName', '박서준',
                    'brideName', '김다은',
                    'weddingDate', '2027-05-22',
                    'weddingTime', '12:30:00'
                     ),
            'venue', JSON_OBJECT(
                    'name', '엘리에나호텔',
                    'hall', '5층 그랜드볼룸',
                    'address', '서울특별시 강남구 논현로 645',
                    'lat', 37.5111132,
                    'lng', 127.0314126,
                    'placeId', NULL,
                    'locationConfirmed', CAST('false' AS JSON)
                     ),
            'coverPhoto', JSON_OBJECT(
                    'id', 'sample-main',
                    'url', '/images/template-samples/color-transparent-v1/wedding-wave.png',
                    'focalX', 50,
                    'focalY', 50,
                    'zoom', 1
                          ),
            'greeting', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '함께 걷는 첫날에 초대해요',
                    'body', CONCAT('서로의 계절이 되어준 두 사람이', CHAR(10), '이제 같은 방향으로 걸어가려 합니다.', CHAR(10), '', CHAR(10), '작은 마음들이 모여 큰 사랑이 된 날,', CHAR(10), '소중한 분들과 그 시작을 함께하고 싶습니다.')
                        ),
            'gallery', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photos', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'sample-bobBride',
                                    'url', '/images/template-samples/color-transparent-v1/bob-bride-bouquet.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-casualEmbrace',
                                    'url', '/images/template-samples/color-transparent-v1/casual-close-embrace.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-standingEmbrace',
                                    'url', '/images/template-samples/color-transparent-v1/standing-embrace.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-seated',
                                    'url', '/images/template-samples/color-transparent-v1/seated-couple.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-cycling',
                                    'url', '/images/template-samples/color-transparent-v1/cycling-together.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-bouquet',
                                    'url', '/images/template-samples/color-transparent-v1/bouquet-standing.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            )
                              )
                       ),
            'parents', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'parent-1',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'name', '박영수',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-2',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'name', '박미정',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-3',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'name', '김정호',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-4',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'name', '최은희',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                       ),
            'contacts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'name', '박서준',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'name', '김다은',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                        ),
            'accounts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-account-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박서준'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-father',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박영수'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-mother',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박미정'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김다은'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-father',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김정호'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-mother',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '최은희'
                            )
                             )
                        ),
            'transport', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'transit', '7호선 학동역 4번 출구에서 도보로 이동하실 수 있어요.',
                    'parking', '주차는 호텔 현장 안내를 따라 주세요.',
                    'shuttle', ''
                         ),
            'notices', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-notice-meal',
                                    'title', '식사 안내',
                                    'body', '오전 11시 30분부터 오후 2시 30분까지 식사를 준비했어요. 도착하시면 안내 데스크에서 식권을 받아 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-children',
                                    'title', '아이와 함께 오시는 분께',
                                    'body', '어린이 의자와 유아 휴게 공간이 준비되어 있어요. 도움이 필요하시면 가까운 안내 직원에게 말씀해 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-flowers',
                                    'title', '마음만으로도 감사해요',
                                    'body', '축하 화환은 정중히 사양해요. 귀한 걸음과 따뜻한 마음만으로도 두 사람에게 큰 선물이 될 거예요.'
                            )
                             )
                       ),
            'interview', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '두 사람의 이야기',
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-interview-1',
                                    'question', '우리는 어떻게 처음 만났나요?',
                                    'groomAnswer', '비가 내리던 봄날, 우연히 함께 쓰게 된 우산 아래에서 처음 이야기를 나누었어요.',
                                    'brideAnswer', '사소한 하루를 특별하게 만들어 주는 사람이라는 걸 그날부터 알았어요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-2',
                                    'question', '함께할 때 가장 행복한 순간은 언제인가요?',
                                    'groomAnswer', '주말 아침 나란히 커피를 마시며 아무 계획 없이 하루를 시작할 때요. 평범한 시간이 가장 오래 기억에 남아요.',
                                    'brideAnswer', '하루를 마치고 서로의 이야기를 들어줄 때요. 함께 웃고 고개를 끄덕여 주는 사람이 있다는 게 참 든든해요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-3',
                                    'question', '앞으로 어떤 부부가 되고 싶나요?',
                                    'groomAnswer', '작은 일에도 고마움을 전하고, 서로의 꿈을 가장 가까이에서 응원하는 사람이 되고 싶어요.',
                                    'brideAnswer', '서로 다른 속도도 다정하게 기다려 주고 싶어요. 오래도록 친구 같은 부부로 함께 걸어갈게요.'
                            )
                             )
                         ),
            'rsvp', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '참석 여부를 알려주세요',
                    'description', '소중한 분들을 더 정성껏 모실 수 있도록 참석 여부를 알려주시면 감사하겠습니다.'
                    ),
            'guestbook', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photo', JSON_OBJECT(
                            'id', 'sample-guestbook',
                            'url', '/images/template-samples/color-transparent-v1/back-to-back.png',
                            'focalX', 50,
                            'focalY', 50,
                            'zoom', 1
                             )
                         ),
            'share', JSON_OBJECT(
                    'title', '서준과 다은의 첫 장에 초대합니다',
                    'description', '2027년 5월 22일 토요일 낮 12시 30분, 두 사람의 새로운 시작을 함께해 주세요.'
                     ),
            'accent', '#212427'
    ),
    1
),

-- 페이퍼 커버 (paper-cover)
(
    'paper-cover',
    '페이퍼 커버',
    '한 권의 이야기처럼',
    '정사각형 사진과 세로로 흐르는 레터링, 단정한 여백이 만나는 초대예요.',
    'minimal',
    JSON_ARRAY(
            '미니멀',
            '사진 중심'
    ),
    '/images/template-samples/color-transparent-v1/bouquet-standing.png',
    'cafe24-oneprettynight',
    JSON_OBJECT(
            'font', 'cafe24-oneprettynight',
            'basic', JSON_OBJECT(
                    'groomName', '박서준',
                    'brideName', '김다은',
                    'weddingDate', '2027-05-22',
                    'weddingTime', '12:30:00'
                     ),
            'venue', JSON_OBJECT(
                    'name', '엘리에나호텔',
                    'hall', '5층 그랜드볼룸',
                    'address', '서울특별시 강남구 논현로 645',
                    'lat', 37.5111132,
                    'lng', 127.0314126,
                    'placeId', NULL,
                    'locationConfirmed', CAST('false' AS JSON)
                     ),
            'coverPhoto', JSON_OBJECT(
                    'id', 'sample-main',
                    'url', '/images/template-samples/color-transparent-v1/bouquet-standing.png',
                    'focalX', 50,
                    'focalY', 50,
                    'zoom', 1
                          ),
            'greeting', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '함께 걷는 첫날에 초대해요',
                    'body', CONCAT('서로의 계절이 되어준 두 사람이', CHAR(10), '이제 같은 방향으로 걸어가려 합니다.', CHAR(10), '', CHAR(10), '작은 마음들이 모여 큰 사랑이 된 날,', CHAR(10), '소중한 분들과 그 시작을 함께하고 싶습니다.')
                        ),
            'gallery', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photos', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'sample-weddingWave',
                                    'url', '/images/template-samples/color-transparent-v1/wedding-wave.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-openArms',
                                    'url', '/images/template-samples/color-transparent-v1/open-arms-walk.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-leaningBouquet',
                                    'url', '/images/template-samples/color-transparent-v1/leaning-bouquet.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-bobBride',
                                    'url', '/images/template-samples/color-transparent-v1/bob-bride-bouquet.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-casualEmbrace',
                                    'url', '/images/template-samples/color-transparent-v1/casual-close-embrace.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-standingEmbrace',
                                    'url', '/images/template-samples/color-transparent-v1/standing-embrace.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            )
                              )
                       ),
            'parents', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'parent-1',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'name', '박영수',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-2',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'name', '박미정',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-3',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'name', '김정호',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-4',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'name', '최은희',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                       ),
            'contacts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'name', '박서준',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'name', '김다은',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                        ),
            'accounts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-account-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박서준'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-father',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박영수'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-mother',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박미정'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김다은'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-father',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김정호'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-mother',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '최은희'
                            )
                             )
                        ),
            'transport', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'transit', '7호선 학동역 4번 출구에서 도보로 이동하실 수 있어요.',
                    'parking', '주차는 호텔 현장 안내를 따라 주세요.',
                    'shuttle', ''
                         ),
            'notices', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-notice-meal',
                                    'title', '식사 안내',
                                    'body', '오전 11시 30분부터 오후 2시 30분까지 식사를 준비했어요. 도착하시면 안내 데스크에서 식권을 받아 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-children',
                                    'title', '아이와 함께 오시는 분께',
                                    'body', '어린이 의자와 유아 휴게 공간이 준비되어 있어요. 도움이 필요하시면 가까운 안내 직원에게 말씀해 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-flowers',
                                    'title', '마음만으로도 감사해요',
                                    'body', '축하 화환은 정중히 사양해요. 귀한 걸음과 따뜻한 마음만으로도 두 사람에게 큰 선물이 될 거예요.'
                            )
                             )
                       ),
            'interview', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '두 사람의 이야기',
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-interview-1',
                                    'question', '우리는 어떻게 처음 만났나요?',
                                    'groomAnswer', '비가 내리던 봄날, 우연히 함께 쓰게 된 우산 아래에서 처음 이야기를 나누었어요.',
                                    'brideAnswer', '사소한 하루를 특별하게 만들어 주는 사람이라는 걸 그날부터 알았어요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-2',
                                    'question', '함께할 때 가장 행복한 순간은 언제인가요?',
                                    'groomAnswer', '주말 아침 나란히 커피를 마시며 아무 계획 없이 하루를 시작할 때요. 평범한 시간이 가장 오래 기억에 남아요.',
                                    'brideAnswer', '하루를 마치고 서로의 이야기를 들어줄 때요. 함께 웃고 고개를 끄덕여 주는 사람이 있다는 게 참 든든해요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-3',
                                    'question', '앞으로 어떤 부부가 되고 싶나요?',
                                    'groomAnswer', '작은 일에도 고마움을 전하고, 서로의 꿈을 가장 가까이에서 응원하는 사람이 되고 싶어요.',
                                    'brideAnswer', '서로 다른 속도도 다정하게 기다려 주고 싶어요. 오래도록 친구 같은 부부로 함께 걸어갈게요.'
                            )
                             )
                         ),
            'rsvp', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '참석 여부를 알려주세요',
                    'description', '소중한 분들을 더 정성껏 모실 수 있도록 참석 여부를 알려주시면 감사하겠습니다.'
                    ),
            'guestbook', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photo', JSON_OBJECT(
                            'id', 'sample-guestbook',
                            'url', '/images/template-samples/color-transparent-v1/seated-couple.png',
                            'focalX', 50,
                            'focalY', 50,
                            'zoom', 1
                             )
                         ),
            'share', JSON_OBJECT(
                    'title', '서준과 다은의 첫 장에 초대합니다',
                    'description', '2027년 5월 22일 토요일 낮 12시 30분, 두 사람의 새로운 시작을 함께해 주세요.'
                     ),
            'accent', '#231B07'
    ),
    1
),

-- 포토 레터 (photo-letter)
(
    'photo-letter',
    '포토 레터',
    '사진 속 우리의 첫날',
    '큰 사진과 간결하게 정돈한 예식 정보로 두 사람의 순간을 전해요.',
    'photo',
    JSON_ARRAY(
            '사진 중심',
            '미니멀'
    ),
    '/images/template-samples/color-transparent-v1/leaning-bouquet.png',
    'cafe24-oneprettynight',
    JSON_OBJECT(
            'font', 'cafe24-oneprettynight',
            'basic', JSON_OBJECT(
                    'groomName', '박서준',
                    'brideName', '김다은',
                    'weddingDate', '2027-05-22',
                    'weddingTime', '12:30:00'
                     ),
            'venue', JSON_OBJECT(
                    'name', '엘리에나호텔',
                    'hall', '5층 그랜드볼룸',
                    'address', '서울특별시 강남구 논현로 645',
                    'lat', 37.5111132,
                    'lng', 127.0314126,
                    'placeId', NULL,
                    'locationConfirmed', CAST('false' AS JSON)
                     ),
            'coverPhoto', JSON_OBJECT(
                    'id', 'sample-main',
                    'url', '/images/template-samples/color-transparent-v1/leaning-bouquet.png',
                    'focalX', 50,
                    'focalY', 50,
                    'zoom', 1
                          ),
            'greeting', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '함께 걷는 첫날에 초대해요',
                    'body', CONCAT('서로의 계절이 되어준 두 사람이', CHAR(10), '이제 같은 방향으로 걸어가려 합니다.', CHAR(10), '', CHAR(10), '작은 마음들이 모여 큰 사랑이 된 날,', CHAR(10), '소중한 분들과 그 시작을 함께하고 싶습니다.')
                        ),
            'gallery', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photos', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'sample-openArms',
                                    'url', '/images/template-samples/color-transparent-v1/open-arms-walk.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-bobBride',
                                    'url', '/images/template-samples/color-transparent-v1/bob-bride-bouquet.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-casualEmbrace',
                                    'url', '/images/template-samples/color-transparent-v1/casual-close-embrace.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-standingEmbrace',
                                    'url', '/images/template-samples/color-transparent-v1/standing-embrace.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-seated',
                                    'url', '/images/template-samples/color-transparent-v1/seated-couple.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-cycling',
                                    'url', '/images/template-samples/color-transparent-v1/cycling-together.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            )
                              )
                       ),
            'parents', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'parent-1',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'name', '박영수',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-2',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'name', '박미정',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-3',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'name', '김정호',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-4',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'name', '최은희',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                       ),
            'contacts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'name', '박서준',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'name', '김다은',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                        ),
            'accounts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-account-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박서준'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-father',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박영수'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-mother',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박미정'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김다은'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-father',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김정호'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-mother',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '최은희'
                            )
                             )
                        ),
            'transport', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'transit', '7호선 학동역 4번 출구에서 도보로 이동하실 수 있어요.',
                    'parking', '주차는 호텔 현장 안내를 따라 주세요.',
                    'shuttle', ''
                         ),
            'notices', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-notice-meal',
                                    'title', '식사 안내',
                                    'body', '오전 11시 30분부터 오후 2시 30분까지 식사를 준비했어요. 도착하시면 안내 데스크에서 식권을 받아 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-children',
                                    'title', '아이와 함께 오시는 분께',
                                    'body', '어린이 의자와 유아 휴게 공간이 준비되어 있어요. 도움이 필요하시면 가까운 안내 직원에게 말씀해 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-flowers',
                                    'title', '마음만으로도 감사해요',
                                    'body', '축하 화환은 정중히 사양해요. 귀한 걸음과 따뜻한 마음만으로도 두 사람에게 큰 선물이 될 거예요.'
                            )
                             )
                       ),
            'interview', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '두 사람의 이야기',
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-interview-1',
                                    'question', '우리는 어떻게 처음 만났나요?',
                                    'groomAnswer', '비가 내리던 봄날, 우연히 함께 쓰게 된 우산 아래에서 처음 이야기를 나누었어요.',
                                    'brideAnswer', '사소한 하루를 특별하게 만들어 주는 사람이라는 걸 그날부터 알았어요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-2',
                                    'question', '함께할 때 가장 행복한 순간은 언제인가요?',
                                    'groomAnswer', '주말 아침 나란히 커피를 마시며 아무 계획 없이 하루를 시작할 때요. 평범한 시간이 가장 오래 기억에 남아요.',
                                    'brideAnswer', '하루를 마치고 서로의 이야기를 들어줄 때요. 함께 웃고 고개를 끄덕여 주는 사람이 있다는 게 참 든든해요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-3',
                                    'question', '앞으로 어떤 부부가 되고 싶나요?',
                                    'groomAnswer', '작은 일에도 고마움을 전하고, 서로의 꿈을 가장 가까이에서 응원하는 사람이 되고 싶어요.',
                                    'brideAnswer', '서로 다른 속도도 다정하게 기다려 주고 싶어요. 오래도록 친구 같은 부부로 함께 걸어갈게요.'
                            )
                             )
                         ),
            'rsvp', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '참석 여부를 알려주세요',
                    'description', '소중한 분들을 더 정성껏 모실 수 있도록 참석 여부를 알려주시면 감사하겠습니다.'
                    ),
            'guestbook', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photo', JSON_OBJECT(
                            'id', 'sample-guestbook',
                            'url', '/images/template-samples/color-transparent-v1/bouquet-standing.png',
                            'focalX', 50,
                            'focalY', 50,
                            'zoom', 1
                             )
                         ),
            'share', JSON_OBJECT(
                    'title', '서준과 다은의 첫 장에 초대합니다',
                    'description', '2027년 5월 22일 토요일 낮 12시 30분, 두 사람의 새로운 시작을 함께해 주세요.'
                     ),
            'accent', '#262626'
    ),
    1
),

-- 고요한 약속 (quiet-vow)
(
    'quiet-vow',
    '고요한 약속',
    '두 이름이 머무는 여백',
    '두 사람의 이름과 사진이 차분하게 이어지는 클래식한 화보예요.',
    'minimal',
    JSON_ARRAY(
            '미니멀',
            '사진 중심'
    ),
    '/images/template-samples/color-transparent-v1/back-to-back.png',
    'cafe24-oneprettynight',
    JSON_OBJECT(
            'font', 'cafe24-oneprettynight',
            'basic', JSON_OBJECT(
                    'groomName', '박서준',
                    'brideName', '김다은',
                    'weddingDate', '2027-05-22',
                    'weddingTime', '12:30:00'
                     ),
            'venue', JSON_OBJECT(
                    'name', '엘리에나호텔',
                    'hall', '5층 그랜드볼룸',
                    'address', '서울특별시 강남구 논현로 645',
                    'lat', 37.5111132,
                    'lng', 127.0314126,
                    'placeId', NULL,
                    'locationConfirmed', CAST('false' AS JSON)
                     ),
            'coverPhoto', JSON_OBJECT(
                    'id', 'sample-main',
                    'url', '/images/template-samples/color-transparent-v1/back-to-back.png',
                    'focalX', 50,
                    'focalY', 50,
                    'zoom', 1
                          ),
            'greeting', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '함께 걷는 첫날에 초대해요',
                    'body', CONCAT('서로의 계절이 되어준 두 사람이', CHAR(10), '이제 같은 방향으로 걸어가려 합니다.', CHAR(10), '', CHAR(10), '작은 마음들이 모여 큰 사랑이 된 날,', CHAR(10), '소중한 분들과 그 시작을 함께하고 싶습니다.')
                        ),
            'gallery', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photos', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'sample-seated',
                                    'url', '/images/template-samples/color-transparent-v1/seated-couple.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-cycling',
                                    'url', '/images/template-samples/color-transparent-v1/cycling-together.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-bouquet',
                                    'url', '/images/template-samples/color-transparent-v1/bouquet-standing.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-weddingWave',
                                    'url', '/images/template-samples/color-transparent-v1/wedding-wave.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-openArms',
                                    'url', '/images/template-samples/color-transparent-v1/open-arms-walk.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-leaningBouquet',
                                    'url', '/images/template-samples/color-transparent-v1/leaning-bouquet.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            )
                              )
                       ),
            'parents', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'parent-1',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'name', '박영수',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-2',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'name', '박미정',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-3',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'name', '김정호',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-4',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'name', '최은희',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                       ),
            'contacts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'name', '박서준',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'name', '김다은',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                        ),
            'accounts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-account-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박서준'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-father',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박영수'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-mother',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박미정'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김다은'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-father',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김정호'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-mother',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '최은희'
                            )
                             )
                        ),
            'transport', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'transit', '7호선 학동역 4번 출구에서 도보로 이동하실 수 있어요.',
                    'parking', '주차는 호텔 현장 안내를 따라 주세요.',
                    'shuttle', ''
                         ),
            'notices', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-notice-meal',
                                    'title', '식사 안내',
                                    'body', '오전 11시 30분부터 오후 2시 30분까지 식사를 준비했어요. 도착하시면 안내 데스크에서 식권을 받아 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-children',
                                    'title', '아이와 함께 오시는 분께',
                                    'body', '어린이 의자와 유아 휴게 공간이 준비되어 있어요. 도움이 필요하시면 가까운 안내 직원에게 말씀해 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-flowers',
                                    'title', '마음만으로도 감사해요',
                                    'body', '축하 화환은 정중히 사양해요. 귀한 걸음과 따뜻한 마음만으로도 두 사람에게 큰 선물이 될 거예요.'
                            )
                             )
                       ),
            'interview', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '두 사람의 이야기',
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-interview-1',
                                    'question', '우리는 어떻게 처음 만났나요?',
                                    'groomAnswer', '비가 내리던 봄날, 우연히 함께 쓰게 된 우산 아래에서 처음 이야기를 나누었어요.',
                                    'brideAnswer', '사소한 하루를 특별하게 만들어 주는 사람이라는 걸 그날부터 알았어요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-2',
                                    'question', '함께할 때 가장 행복한 순간은 언제인가요?',
                                    'groomAnswer', '주말 아침 나란히 커피를 마시며 아무 계획 없이 하루를 시작할 때요. 평범한 시간이 가장 오래 기억에 남아요.',
                                    'brideAnswer', '하루를 마치고 서로의 이야기를 들어줄 때요. 함께 웃고 고개를 끄덕여 주는 사람이 있다는 게 참 든든해요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-3',
                                    'question', '앞으로 어떤 부부가 되고 싶나요?',
                                    'groomAnswer', '작은 일에도 고마움을 전하고, 서로의 꿈을 가장 가까이에서 응원하는 사람이 되고 싶어요.',
                                    'brideAnswer', '서로 다른 속도도 다정하게 기다려 주고 싶어요. 오래도록 친구 같은 부부로 함께 걸어갈게요.'
                            )
                             )
                         ),
            'rsvp', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '참석 여부를 알려주세요',
                    'description', '소중한 분들을 더 정성껏 모실 수 있도록 참석 여부를 알려주시면 감사하겠습니다.'
                    ),
            'guestbook', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photo', JSON_OBJECT(
                            'id', 'sample-guestbook',
                            'url', '/images/template-samples/color-transparent-v1/bob-bride-bouquet.png',
                            'focalX', 50,
                            'focalY', 50,
                            'zoom', 1
                             )
                         ),
            'share', JSON_OBJECT(
                    'title', '서준과 다은의 첫 장에 초대합니다',
                    'description', '2027년 5월 22일 토요일 낮 12시 30분, 두 사람의 새로운 시작을 함께해 주세요.'
                     ),
            'accent', '#1C262B'
    ),
    1
),

-- 세이지 프레임 (sage-frame)
(
    'sage-frame',
    '세이지 프레임',
    '한 장의 정갈한 약속',
    '흰 종이 위 세이지빛 보더와 사진을 곱게 올린 초대 카드예요.',
    'minimal',
    JSON_ARRAY(
            '미니멀',
            '사진 중심'
    ),
    '/images/template-samples/color-transparent-v1/bob-bride-bouquet.png',
    'cafe24-oneprettynight',
    JSON_OBJECT(
            'font', 'cafe24-oneprettynight',
            'basic', JSON_OBJECT(
                    'groomName', '박서준',
                    'brideName', '김다은',
                    'weddingDate', '2027-05-22',
                    'weddingTime', '12:30:00'
                     ),
            'venue', JSON_OBJECT(
                    'name', '엘리에나호텔',
                    'hall', '5층 그랜드볼룸',
                    'address', '서울특별시 강남구 논현로 645',
                    'lat', 37.5111132,
                    'lng', 127.0314126,
                    'placeId', NULL,
                    'locationConfirmed', CAST('false' AS JSON)
                     ),
            'coverPhoto', JSON_OBJECT(
                    'id', 'sample-main',
                    'url', '/images/template-samples/color-transparent-v1/bob-bride-bouquet.png',
                    'focalX', 50,
                    'focalY', 50,
                    'zoom', 1
                          ),
            'greeting', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '함께 걷는 첫날에 초대해요',
                    'body', CONCAT('서로의 계절이 되어준 두 사람이', CHAR(10), '이제 같은 방향으로 걸어가려 합니다.', CHAR(10), '', CHAR(10), '작은 마음들이 모여 큰 사랑이 된 날,', CHAR(10), '소중한 분들과 그 시작을 함께하고 싶습니다.')
                        ),
            'gallery', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photos', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'sample-weddingWave',
                                    'url', '/images/template-samples/color-transparent-v1/wedding-wave.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-openArms',
                                    'url', '/images/template-samples/color-transparent-v1/open-arms-walk.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-leaningBouquet',
                                    'url', '/images/template-samples/color-transparent-v1/leaning-bouquet.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-casualEmbrace',
                                    'url', '/images/template-samples/color-transparent-v1/casual-close-embrace.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-standingEmbrace',
                                    'url', '/images/template-samples/color-transparent-v1/standing-embrace.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-seated',
                                    'url', '/images/template-samples/color-transparent-v1/seated-couple.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            )
                              )
                       ),
            'parents', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'parent-1',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'name', '박영수',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-2',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'name', '박미정',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-3',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'name', '김정호',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-4',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'name', '최은희',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                       ),
            'contacts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'name', '박서준',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'name', '김다은',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                        ),
            'accounts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-account-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박서준'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-father',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박영수'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-mother',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박미정'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김다은'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-father',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김정호'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-mother',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '최은희'
                            )
                             )
                        ),
            'transport', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'transit', '7호선 학동역 4번 출구에서 도보로 이동하실 수 있어요.',
                    'parking', '주차는 호텔 현장 안내를 따라 주세요.',
                    'shuttle', ''
                         ),
            'notices', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-notice-meal',
                                    'title', '식사 안내',
                                    'body', '오전 11시 30분부터 오후 2시 30분까지 식사를 준비했어요. 도착하시면 안내 데스크에서 식권을 받아 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-children',
                                    'title', '아이와 함께 오시는 분께',
                                    'body', '어린이 의자와 유아 휴게 공간이 준비되어 있어요. 도움이 필요하시면 가까운 안내 직원에게 말씀해 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-flowers',
                                    'title', '마음만으로도 감사해요',
                                    'body', '축하 화환은 정중히 사양해요. 귀한 걸음과 따뜻한 마음만으로도 두 사람에게 큰 선물이 될 거예요.'
                            )
                             )
                       ),
            'interview', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '두 사람의 이야기',
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-interview-1',
                                    'question', '우리는 어떻게 처음 만났나요?',
                                    'groomAnswer', '비가 내리던 봄날, 우연히 함께 쓰게 된 우산 아래에서 처음 이야기를 나누었어요.',
                                    'brideAnswer', '사소한 하루를 특별하게 만들어 주는 사람이라는 걸 그날부터 알았어요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-2',
                                    'question', '함께할 때 가장 행복한 순간은 언제인가요?',
                                    'groomAnswer', '주말 아침 나란히 커피를 마시며 아무 계획 없이 하루를 시작할 때요. 평범한 시간이 가장 오래 기억에 남아요.',
                                    'brideAnswer', '하루를 마치고 서로의 이야기를 들어줄 때요. 함께 웃고 고개를 끄덕여 주는 사람이 있다는 게 참 든든해요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-3',
                                    'question', '앞으로 어떤 부부가 되고 싶나요?',
                                    'groomAnswer', '작은 일에도 고마움을 전하고, 서로의 꿈을 가장 가까이에서 응원하는 사람이 되고 싶어요.',
                                    'brideAnswer', '서로 다른 속도도 다정하게 기다려 주고 싶어요. 오래도록 친구 같은 부부로 함께 걸어갈게요.'
                            )
                             )
                         ),
            'rsvp', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '참석 여부를 알려주세요',
                    'description', '소중한 분들을 더 정성껏 모실 수 있도록 참석 여부를 알려주시면 감사하겠습니다.'
                    ),
            'guestbook', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photo', JSON_OBJECT(
                            'id', 'sample-guestbook',
                            'url', '/images/template-samples/color-transparent-v1/cycling-together.png',
                            'focalX', 50,
                            'focalY', 50,
                            'zoom', 1
                             )
                         ),
            'share', JSON_OBJECT(
                    'title', '서준과 다은의 첫 장에 초대합니다',
                    'description', '2027년 5월 22일 토요일 낮 12시 30분, 두 사람의 새로운 시작을 함께해 주세요.'
                     ),
            'accent', '#1B2116'
    ),
    1
),

-- 하트 노트 (heart-note)
(
    'heart-note',
    '하트 노트',
    '종이 위에 붙인 작은 사랑',
    '붉은 종이 하트와 한 장의 사진을 종이 위에 붙인 듯한 초대예요. 작은 가운데 소제목과 여백으로 두 사람의 이야기를 차분히 전해요.',
    'handwriting',
    JSON_ARRAY(
            '손글씨',
            '로맨틱'
    ),
    '/images/template-samples/color-transparent-v1/standing-embrace.png',
    'lee-seoyun',
    JSON_OBJECT(
            'font', 'lee-seoyun',
            'basic', JSON_OBJECT(
                    'groomName', '박서준',
                    'brideName', '김다은',
                    'weddingDate', '2027-05-22',
                    'weddingTime', '12:30:00'
                     ),
            'venue', JSON_OBJECT(
                    'name', '엘리에나호텔',
                    'hall', '5층 그랜드볼룸',
                    'address', '서울특별시 강남구 논현로 645',
                    'lat', 37.5111132,
                    'lng', 127.0314126,
                    'placeId', NULL,
                    'locationConfirmed', CAST('false' AS JSON)
                     ),
            'coverPhoto', JSON_OBJECT(
                    'id', 'sample-main',
                    'url', '/images/template-samples/color-transparent-v1/standing-embrace.png',
                    'focalX', 50,
                    'focalY', 50,
                    'zoom', 1
                          ),
            'greeting', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '함께 걷는 첫날에 초대해요',
                    'body', CONCAT('서로의 계절이 되어준 두 사람이', CHAR(10), '이제 같은 방향으로 걸어가려 합니다.', CHAR(10), '', CHAR(10), '작은 마음들이 모여 큰 사랑이 된 날,', CHAR(10), '소중한 분들과 그 시작을 함께하고 싶습니다.')
                        ),
            'gallery', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photos', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'sample-backToBack',
                                    'url', '/images/template-samples/color-transparent-v1/back-to-back.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-weddingWave',
                                    'url', '/images/template-samples/color-transparent-v1/wedding-wave.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-openArms',
                                    'url', '/images/template-samples/color-transparent-v1/open-arms-walk.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-leaningBouquet',
                                    'url', '/images/template-samples/color-transparent-v1/leaning-bouquet.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-bobBride',
                                    'url', '/images/template-samples/color-transparent-v1/bob-bride-bouquet.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            ),
                            JSON_OBJECT(
                                    'id', 'sample-casualEmbrace',
                                    'url', '/images/template-samples/color-transparent-v1/casual-close-embrace.png',
                                    'focalX', 50,
                                    'focalY', 50,
                                    'zoom', 1
                            )
                              )
                       ),
            'parents', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'parent-1',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'name', '박영수',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-2',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'name', '박미정',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-3',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'name', '김정호',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'parent-4',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'name', '최은희',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                       ),
            'contacts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'name', '박서준',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'name', '김다은',
                                    'phone', '010-0000-0000',
                                    'public', CAST('true' AS JSON)
                            )
                             )
                        ),
            'accounts', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-account-groom',
                                    'side', 'groom',
                                    'relation', '신랑',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박서준'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-father',
                                    'side', 'groom',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박영수'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-groom-mother',
                                    'side', 'groom',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '박미정'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride',
                                    'side', 'bride',
                                    'relation', '신부',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김다은'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-father',
                                    'side', 'bride',
                                    'relation', '아버지',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '김정호'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-account-bride-mother',
                                    'side', 'bride',
                                    'relation', '어머니',
                                    'bank', '견본은행',
                                    'number', '000-0000-0000',
                                    'holder', '최은희'
                            )
                             )
                        ),
            'transport', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'transit', '7호선 학동역 4번 출구에서 도보로 이동하실 수 있어요.',
                    'parking', '주차는 호텔 현장 안내를 따라 주세요.',
                    'shuttle', ''
                         ),
            'notices', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-notice-meal',
                                    'title', '식사 안내',
                                    'body', '오전 11시 30분부터 오후 2시 30분까지 식사를 준비했어요. 도착하시면 안내 데스크에서 식권을 받아 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-children',
                                    'title', '아이와 함께 오시는 분께',
                                    'body', '어린이 의자와 유아 휴게 공간이 준비되어 있어요. 도움이 필요하시면 가까운 안내 직원에게 말씀해 주세요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-notice-flowers',
                                    'title', '마음만으로도 감사해요',
                                    'body', '축하 화환은 정중히 사양해요. 귀한 걸음과 따뜻한 마음만으로도 두 사람에게 큰 선물이 될 거예요.'
                            )
                             )
                       ),
            'interview', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '두 사람의 이야기',
                    'items', JSON_ARRAY(
                            JSON_OBJECT(
                                    'id', 'complete-interview-1',
                                    'question', '우리는 어떻게 처음 만났나요?',
                                    'groomAnswer', '비가 내리던 봄날, 우연히 함께 쓰게 된 우산 아래에서 처음 이야기를 나누었어요.',
                                    'brideAnswer', '사소한 하루를 특별하게 만들어 주는 사람이라는 걸 그날부터 알았어요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-2',
                                    'question', '함께할 때 가장 행복한 순간은 언제인가요?',
                                    'groomAnswer', '주말 아침 나란히 커피를 마시며 아무 계획 없이 하루를 시작할 때요. 평범한 시간이 가장 오래 기억에 남아요.',
                                    'brideAnswer', '하루를 마치고 서로의 이야기를 들어줄 때요. 함께 웃고 고개를 끄덕여 주는 사람이 있다는 게 참 든든해요.'
                            ),
                            JSON_OBJECT(
                                    'id', 'complete-interview-3',
                                    'question', '앞으로 어떤 부부가 되고 싶나요?',
                                    'groomAnswer', '작은 일에도 고마움을 전하고, 서로의 꿈을 가장 가까이에서 응원하는 사람이 되고 싶어요.',
                                    'brideAnswer', '서로 다른 속도도 다정하게 기다려 주고 싶어요. 오래도록 친구 같은 부부로 함께 걸어갈게요.'
                            )
                             )
                         ),
            'rsvp', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'title', '참석 여부를 알려주세요',
                    'description', '소중한 분들을 더 정성껏 모실 수 있도록 참석 여부를 알려주시면 감사하겠습니다.'
                    ),
            'guestbook', JSON_OBJECT(
                    'enabled', CAST('true' AS JSON),
                    'photo', JSON_OBJECT(
                            'id', 'sample-guestbook',
                            'url', '/images/template-samples/color-transparent-v1/seated-couple.png',
                            'focalX', 50,
                            'focalY', 50,
                            'zoom', 1
                             )
                         ),
            'share', JSON_OBJECT(
                    'title', '서준과 다은의 첫 장에 초대합니다',
                    'description', '2027년 5월 22일 토요일 낮 12시 30분, 두 사람의 새로운 시작을 함께해 주세요.'
                     ),
            'accent', '#BF2942'
    ),
    1
);

COMMIT;
