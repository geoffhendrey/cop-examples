import express from 'express';
import axios from 'axios';
import AWS from 'aws-sdk';
import bodyParser from 'body-parser';

const app = express();
const port = 8080;

const knowledgeStoreUrl = process.env.APPD_JSON_STORE_URL || 'https://arch3.saas.appd-test.com/knowledge-store';
const token = process.env.TOKEN;

// Headers to be propagated
const propagatedHeaders = [
  'appd-cpty',
  'appd-cpid',
  'layer-type',
  'layer-id',
  'traceparent',
  'x-b3-parentspanid',
  'x-b3-spanid',
  'x-b3-traceid',
  'x-request-id',
];

// app.use((req, res, next) => {
//   console.log('Incoming request:', req.method, req.path);
//   console.log('Headers:', req.headers);
//   console.log('Body:', req.body);
//   next();
// });

app.use(bodyParser.json());

app.post('/', async (req, res) => {
  try {
    const { id } = req.body;
    if (!id) {
      return res.status(400).send('ID is required');
    }

    // Extract and propagate specific headers
    const requestHeaders = Object.keys(req.headers)
      .filter(header => propagatedHeaders.includes(header.toLowerCase()))
      .reduce((acc, header) => ({ ...acc, [header]: req.headers[header] }), {});


    requestHeaders['Accept'] = 'application/json';
    requestHeaders['Content-Type'] = 'application/json';
    requestHeaders['Includetags'] = 'false';
    requestHeaders['Layer-Id'] = '2d4866c4-0a45-41ec-a534-011e5f4d970a';
    requestHeaders['Layer-Type'] = 'TENANT';
    requestHeaders['appd-pty'] = 'IlVTRVIi';
    requestHeaders['appd-pid'] = 'mZvb0BiYXIuY29tIg==';

    // this header only works in the zodiac function environment.
    // Comment it out for testing on your local laptop
    requestHeaders['appd-cpty'] = 'SOLUTION';

    // Optionally add a token if it exists in the environment variables
    if (token) {
      requestHeaders['Authorization'] = `Bearer ${token}`;
    }

    const credentials = await fetchCredentialsFromKnowledgeStore(id, requestHeaders);
    if (!credentials) {
      return res.status(404).send('Credentials not found');
    }

    // const s3Buckets = await listAllS3Buckets(credentials);
    res.json(credentials);
  } catch (error) {
    console.error('An error occurred:', error);
    res.status(500).send(error.message);
  }
});

const fetchCredentialsFromKnowledgeStore = async (id, headers) => {
  try {
    const response = await axios.get(`${knowledgeStoreUrl}/v1/objects/${id}`, { headers });
    return response.data;
  } catch (error) {
    console.error('Error fetching credentials from knowledge store:', error);
    throw error;
  }
};

const listAllS3Buckets = async (credentials) => {
  AWS.config.update({
    accessKeyId: credentials.id,
    secretAccessKey: credentials.key,
    region: credentials.region || 'us-west-2',
  });

  const s3 = new AWS.S3();
  try {
    const data = await s3.listBuckets().promise();
    return data.Buckets.map(bucket => bucket.Name);
  } catch (error) {
    console.error('Error listing S3 buckets:', error);
    throw error;
  }
};

app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});
