curl -i -sS -X POST \
  -H 'User-Agent: Dalvik/2.1.0 (Linux; U; Android 13; V1911A Build/TP1A.220905.001)' \
  -H 'Content-Type: application/encrypted-json' \
  -H 'Content-Length: 1136' \
  -H 'Accept-Encoding: gzip' \
  -H 'Connection: Keep-Alive' \
  -H 'Accept:' \
  --data-binary @body.bin \
  'http://127.0.0.1:8080/registerv3?ver=3.0&model=V1911A&manuf=vivo&sign=W3NyYVi%2BeZW8nGp3UZztHq9CkjKo6pRFpebAbJmECL6fJibbjLNsx0H6tj4agEHxk9msyDiYj0f4npB97nhSAtaWbFrHxeV9yxSSLLpy4lFC%2Fo9YQ59iWsTzp9M7jEDVLksh2WB1OZ8dotzyiP23Hyj3F%2B4bZCraLS%2B34gQ36%2FE%3D'
