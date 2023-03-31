# Analysis (WIP)

## Modules

Enable
- Contact
- Date time
- Date time range
- Facets
- Field group
- HAL
- HTTP Basic Authentication
- Geofield
- JSON:API
- Metatag: Dublin Core
- Metatag: Dublin Core Advanced
- Openfed multilingual
- Openfed social links
- Optional end date
- Path
- RDF
- Search API Autocomplete
- Search API Solr
- Search API Solr Admin
- Search API Solr Autocomplete
- Serialization
- Simple XML Sitemap
- Simple XML Sitemap Search Engines
- Views
- Weight

### JSON:API

Setting: `Accept all JSON:API create, read, update, and delete operations.`

## Taxonomies

### Taxonomy: Application Types

To be used in examples of reuse, e.g. "Blog post"

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

Values:
- API
- application
- article
- blog post
- dashboard
- visualization
- website

### Taxonomy: Contact Types

To be used in default contact form, e.g. "Other question"

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

Values:
- Complaint
- Information / Dataset request
- New application / Dataset reuse
- Studies / dissertations
- Harvesting / Register own dataset
- Other question

### Taxonomy: Data Categories

E.g "Transport"

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

Values:
- Agriculture and Fisheries
- Culture and Sports
- Economy and Finance
- Education
- Energy
- Environment
- Health
- International
- Justice
- Population
- Public Sector
- Regional
- Science and Technology
- Transport


### Taxonomy: File Types

Language-independent, e.g. "CSV"

URI is not visible in front-end, used for mapping with other sources / portals.

| Field | Type | Required |
| --- | --- | --- |
| Title | String | yes |
| URI | Link | yes |

Values:
- ARCGIS
- ATOM
- CSV
- DBF
- DOC
- DOCX
- DWC-A
- HTML
- GeoJSON
- GeoPKG
- GeoTIFF
- GML
- GTFS
- JSON
- JSON-LD
- KML
- MDB
- ODS
- PDF
- RDF/XML
- RSS
- SHP
- SLD
- SQLite
- TIFF
- TSV
- TTL
- TXT
- WCS
- WFS
- WMS
- XLS
- XLSX
- XML

### Taxonomy: Compression Types

Language-independent, e.g. "ZIP"

URI is not visible in front-end, used for mapping with other sources / portals.

| Field | Type | Required |
| --- | --- | --- |
| Title | String | yes |
| URI | Link | yes |

Values
- GZIP
- ZIP


### Taxonomy: Geographies

Tree-structure, e.g "Flanders > Ghent"

URI is not visible in front-end, used for mapping with other sources / portals.

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes  | yes |
| Bounding box | Geofield (WKT) | yes | no |
| URI | Link | yes | no |

Values:

- Belgium
  - Flanders
    - Antwerp
    - Bruges
    - Ghent
    - Hasselt
    - Kortrijk
    - Leuven
    - Ostend
  - Brussels
  - Wallonia
    - Arlon
    - Charleroi
    - Liège
    - Mons
    - Namur

### Taxonomy: Licenses

Tree-structure, e.g "Open > Creative Commons"

URI is not visible in front-end, used for mapping with other sources / portals.

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

### Taxonomy: Organizations

Tree-structure, e.g "Federal > FPS BOSA"

URI is not visible in front-end, used for mapping with other sources / portals.

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

Values:

- Federal
- Interfederal
- Flemish Region
- Brussels-Capital Region
- Walloon Region
- French Community
- German-speaking community
- Provinces
- Municipalities
- Universities
- Companies and NPOs

### Taxonomy: Update Frequencies

E.g. "Weekly"

URI is not visible in front-end, used for mapping with other sources / portals.

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

Values:

- Continuously
- Hourly
- Daily
- Weekly
- Monthly
- Quarterly
- Anually
- Decennially
- Irregurarly
- Unknown
- Not planned
## Content types

### Content type: Application

Example of re-use of open data sets / services

| Field | Type | Required | Translatable | Multiple |
| --- | --- | --- | --- | --- |
| Title | String | yes | yes | no |
| Body | Text field | yes | yes | no |
| Image | Image | no | no | no |
| Website | Link | yes | yes | no |
| Category | Taxonomy: Data Categories | yes | -- | yes |
| Type | Taxonomy: Application Types | yes | -- | no |
| Reuse | Link | no | yes | no |

### Content type: Dataset

Metadata about an open dataset or service (note that the files themselves are hosted elsewhere, no attachments)

URI is not visible in front-end, used for mapping with other sources / portals.

Last modified date is the last update of the dataset, this is different from the update date of the Drupal node.

Author is not the Drupal user creating the content, but a person's / team's name or name of an organization.

Web page(s), download link(s) can sometimes be different in different languages.


| Field | Type | Required | Translatable | Multiple | Display | Search |
| --- | --- | --- | --- | --- | --- | --- |
| Title | String | yes | yes | no | text | full text search |
| Body | Text field | yes | yes | no | text | full text search |
| URI | Link | yes | no | no | hidden | no |
| Update frequency | Taxonomy: Update Frequencies | no | -- | no | text | facet |
| Last modified | Timestamp | no | no | no | date | no |
| From / till | Date range | no | no | no | date range | facet |
| Author | String | yes | yes | yes | text | no |
| Publisher | Taxonomy: Organizations | yes | -- | no | link | facet |
| Contact e-mail | E-mail | no | yes | no | link | no |
| Contact form | Link (external) | no | yes | no | link | no |
| Geography | Taxonomy: Geographies | yes | -- | yes | text | facet |
| Category | Taxonomy: Data Categories | yes | -- | yes | text | facet |
| License | Taxonomy: Licenses | yes | -- | yes | text | facet |
| Format | Taxonomy: File types | yes | -- | yes | text | facet |
| Web page | Link (external) | yes | yes, no sync | yes | link | no |
| Download URL | Link (external) | no | yes, no sync | yes | link | no |
| Service URL | Link (external) | no | yes | yes | link | no |
| Keyword | String | no | yes, no sync | yes | hidden | full text search |
| High Value Dataset | boolean | yes | no | no | text | facet |
| API| boolean | yes | no | no | text | facet |


### Content type: News

Short news item about an event or new dataset

| Field | Type | Required | Translatable | Multiple |
| --- | --- | --- | --- | --- |
| Title | String | yes | yes | no |
| Description | Text field | yes | yes | no |
| Image | Image | no | no | no |
| Website | Link | no | yes | no |

### Content type: Page

Basic web page, used for documentation in general

| Field | Type | Required | Translatable | Multiple |
| --- | --- | --- | --- | --- |
| Title | String | yes | yes | no |
| Description | Text field | yes | yes | no |

## Forms

### Contact form

Default contact form

| Field | Type | Required | Display |
| --- | --- | --- | --- |
| Your name | String | yes | single line |
| Your e-mail | Email | yes | single line |
| Category | Taxonomy: Contact Types | yes | select list |
| Subject | String | yes | string |
| Message | Text field | yes | multi-line text field |
| Agree policy | Boolean | yes | checkbox |

## Views

### RSS feed

Paginated view of all the Datasets

### High value datasets

Paginated list of all datasets with "high-value dataset" set to true.

### Content view with data (backend)

Paginated table of all content (only used/visible by site admin).
With the possibility of searching on all these fields

| NodeID | Title | Language | Content type | Author | Updated date |

Ascending/descending ordering must be possible on Title or Updated date


### News ordered per date

Paginated grid of news, newest item first

- Filter
   - Content type is News
- Page settings
   - Path: /news
- Language
  - Rendering Language: Interface text language selected for page
- Pager
  - Mini: 10 items page
  - More link: yes

### Applications ordered per date

Paginated grid of re-use of datasets, newest items first.

- Filter
   - Content type is Application
- Page settings
   - Path: /apps
- Language
  - Rendering Language: Interface text language selected for page
- Pager
  - Mini: 10 items page
  - More link: yes

## Search

### Solr Index

Fields

- Title
- Body
- Language
- Keyword
- Taxonomy: Application Types
- Taxonomy: Data Categories
- Taxonomy: File Types
- Taxonomy: Geographies (with parent)
- Taxonomy: Licenses
- Taxonomy: Organizations (with parent)

### Datasets search

Faceted search + free text.

For the tree-structures Geographies and Organizations, results of the children must be included in the search.
E.g searching on Geographies: Flanders should also return the results for Ghent, 
searching for Organization: federal should also return the results for FPS BOSA

- Taxonomy: Data Categories
- Taxonomy: File Types
- Taxonomy: Geographies (with parent)
- Taxonomy: Licenses
- Taxonomy: Organizations (with parent)


## Menus

### Top

- Home
- Apps (link to applications)
- Data (link to datasets search)
- News (link to latest news)
- Info & FAQ (generic documentation)
- Contact

### Bottom

- Terms of use
- Accessibility statement
- Cookie policy
- Privavy statement
- Link to github tools
